/*
 * Copyright (C) 2026 Sopra Steria Group.
 *
 * This file is part of the sword-le project:
 * https://github.com/soprasteria/wsq4j
 *
 * Licenced under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder;

import io.github.soprasteria.wsq4j.domain.constants.NISTConstants;
import io.github.soprasteria.wsq4j.domain.constants.WSQConstants;
import io.github.soprasteria.wsq4j.domain.entities.Bitmap;
import io.github.soprasteria.wsq4j.domain.entities.BitmapWithMetadata;
import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jEncodeException;
import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jInvalidArgumentException;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.Wsq4jConfig;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("DuplicatedCode")
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WSQEncoder implements WSQConstants, NISTConstants {

  public static void encode(OutputStream os, Bitmap bitmap, double bitRate, String... comments)
      throws IOException {
    encode(os, bitmap, bitRate, null, comments);
  }

  @SuppressWarnings("unused")
  public static void encode(
      DataOutput dataOutput, Bitmap bitmap, double bitRate, String... comments) throws IOException {
    encode(dataOutput, bitmap, bitRate, null, comments);
  }

  public static void encode(
      OutputStream os,
      Bitmap bitmap,
      double bitRate,
      Map<String, String> metadata,
      String... comments)
      throws IOException {
    encode((DataOutput) new DataOutputStream(os), bitmap, bitRate, metadata, comments);
  }

  public static void encode(
      DataOutput dataOutput,
      Bitmap _bitmap,
      double bitRate,
      Map<String, String> metadata,
      String... comments)
      throws IOException {

    verifyParamsOrThrow(_bitmap, bitRate);
    BitmapWithMetadata bitmap;
    if (_bitmap instanceof BitmapWithMetadata) {
      bitmap = (BitmapWithMetadata) _bitmap;
    } else {
      bitmap =
          new BitmapWithMetadata(
              _bitmap.getPixels(),
              _bitmap.getWidth(),
              _bitmap.getHeight(),
              _bitmap.getPpi(),
              _bitmap.getDepth(),
              _bitmap.getLossyflag());
    }
    if (metadata != null) {
      bitmap.getMetadata().putAll(metadata);
    }
    if (comments != null) {
      for (String s : comments) {
        if (s != null) {
          bitmap.getComments().add(s);
        }
      }
    }

    final int num_pix = bitmap.getWidth() * bitmap.getHeight();
    float[] fdata; /* floating point pixel image  */
    int[] qdata; /* quantized image pointer     */
    WSQHelper.Ref<Integer> qsize = new WSQHelper.Ref<>();
    WSQHelper.Ref<Integer> qsize1 = new WSQHelper.Ref<>();
    WSQHelper.Ref<Integer> qsize2 = new WSQHelper.Ref<>();
    WSQHelper.Ref<Integer> qsize3 = new WSQHelper.Ref<>(); /* quantized block sizes */
    WSQHelper.HuffCode[] hufftable;
    WSQHelper.Ref<int[]> huffbits = new WSQHelper.Ref<>();
    WSQHelper.Ref<int[]> huffvalues = new WSQHelper.Ref<>(); /* huffman code parameters */
    int hsize, hsize1, hsize2, hsize3; /* Huffman coded blocks sizes */

    /* Convert image pixels to floating point. */
    WSQHelper.Ref<Float> m_shift = new WSQHelper.Ref<>(), r_scale = new WSQHelper.Ref<>();
    fdata = conv_img_2_flt(bitmap.getPixels(), m_shift, r_scale);

    log.debug("Input image pixels converted to floating point");
    WSQHelper.Token token = new WSQHelper.Token();

    /* Build WSQ decomposition trees */
    WSQHelper.buildWSQTrees(token, bitmap.getWidth(), bitmap.getHeight());
    log.debug("Tables for wavelet decomposition finished");

    /* WSQ decompose the image */
    wsqDecompose(
        token,
        fdata,
        bitmap.getWidth(),
        bitmap.getHeight(),
        token.tableDTT.hifilt,
        MAX_HIFILT,
        token.tableDTT.lofilt,
        MAX_LOFILT);
    log.debug("WSQ decomposition of image finished");

    /* Set compression ratio and 'q' to zero. */
    token.quant_vals.cr = 0;
    token.quant_vals.q = 0.0f;

    /* Assign specified r-bitrate into quantization structure. */
    token.quant_vals.r = (float) Math.rint(bitRate * 1e6) / 1e6f;

    /* Compute subband variances. */
    variance(token, fdata, bitmap.getWidth(), bitmap.getHeight());
    log.debug("Subband variances computed");

    /* Quantize the floating point pixmap. */
    qdata = quantize(token, qsize, fdata, bitmap.getWidth(), bitmap.getHeight());
    log.debug("WSQ subband decomposition data quantized");

    /* Compute quantized WSQ subband block sizes */
    quant_block_sizes(token, qsize1, qsize2, qsize3);

    if (qsize.value != qsize1.value + qsize2.value + qsize3.value) {
      throw new Wsq4jEncodeException("wsq_encode_1 : problem w/quantization block sizes");
    }

    /* Add a Start Of Image (SOI) marker to the WSQ buffer. */
    dataOutput.writeShort(SOI_WSQ);

    putc_nistcom_wsq(dataOutput, bitmap, (float) bitRate, metadata, comments);

    /* Store the Wavelet filter taps to the WSQ buffer. */
    putc_transform_table(
        dataOutput, token.tableDTT.lofilt, MAX_LOFILT, token.tableDTT.hifilt, MAX_HIFILT);

    /* Store the quantization parameters to the WSQ buffer. */
    putc_quantization_table(dataOutput, token);

    /* Store a frame header to the WSQ buffer. */
    int tailleHeader =
        putc_frame_header_wsq(
            dataOutput, bitmap.getWidth(), bitmap.getHeight(), m_shift.value, r_scale.value);
    log.debug("SOI, tables, and frame header written with header size: {}", tailleHeader);

    {
      /* ************** */
      /* ENCODE Block 1 */
      /* ************** */
      log.debug("ENCODE Block 1");

      debug_qdata(qdata);
      /* Compute Huffman table for Block 1. */
      hufftable = gen_hufftable_wsq(huffbits, huffvalues, qdata, 0, new int[] {qsize1.value});
      debugHuffmanCodes(hufftable);

      /* Store Huffman table for Block 1 to WSQ buffer. */
      int tailleDHT = putc_huffman_table(dataOutput, 0, huffbits.value, huffvalues.value);
      log.debug("Huffman code Table 1 generated and written with size:{}", tailleDHT);
      debug_huffbits(huffbits);

      /* Compress Block 1 data. */
      byte[] huff_buf =
          compress_block(qdata, 0, qsize1.value, MAX_HUFFCOEFF, MAX_HUFFZRUN, hufftable);
      hsize1 = huff_buf.length;
      log.debug("block len : {}", hsize1);

      /* Accumulate number of bytes compressed. */
      hsize = hsize1;

      /* Store Block 1's header to WSQ buffer. */
      int headerSize = putc_block_header(dataOutput, 0);

      /* Store Block 1's compressed data to WSQ buffer. */
      dataOutput.write(huff_buf);
      log.debug(
          "Block 1 compressed and written header size: {} block size: {}", headerSize, hsize1);
    }
    {
      /* ************** */
      /* ENCODE Block 2 */
      /* ************** */
      log.debug("ENCODE Block 2");

      debug_qdata(qdata);
      /* Compute  Huffman table for Blocks 2 & 3. */
      hufftable =
          gen_hufftable_wsq(
              huffbits, huffvalues, qdata, qsize1.value, new int[] {qsize2.value, qsize3.value});
      debugHuffmanCodes(hufftable);

      /* Store Huffman table for Blocks 2 & 3 to WSQ buffer. */
      int tailleDHT = putc_huffman_table(dataOutput, 1, huffbits.value, huffvalues.value);
      log.debug("Huffman code Table 2 generated and written size: {}", tailleDHT);

      /* Compress Block 2 data. */
      byte[] huff_buf =
          compress_block(qdata, qsize1.value, qsize2.value, MAX_HUFFCOEFF, MAX_HUFFZRUN, hufftable);
      hsize2 = huff_buf.length;

      /* Accumulate number of bytes compressed. */
      hsize += hsize2;

      /* Store Block 2's header to WSQ buffer. */
      int headerSize = putc_block_header(dataOutput, 1);

      dataOutput.write(huff_buf);
      log.debug(
          "Block 2 compressed and written header size: {} block size: {}", headerSize, hsize2);
    }
    {
      /* ************** */
      /* ENCODE Block 3 */
      /* ************** */
      log.debug("ENCODE Block 3");

      /* Compress Block 3 data. */
      byte[] huff_buf =
          compress_block(
              qdata,
              qsize1.value + qsize2.value,
              qsize3.value,
              MAX_HUFFCOEFF,
              MAX_HUFFZRUN,
              hufftable);
      hsize3 = huff_buf.length;

      /* Accumulate number of bytes compressed. */
      hsize += hsize3;

      /* Store Block 3's header to WSQ buffer. */
      int headerSize = putc_block_header(dataOutput, 1);

      dataOutput.write(huff_buf);
      log.debug(
          "Block 3 compressed and written header size: {} block size: {}", headerSize, hsize3);
    }

    /* Add an End Of Image (EOI) marker to the WSQ buffer. */
    dataOutput.writeShort(EOI_WSQ);
    if (log.isDebugEnabled()) {
      log.debug("hsize1 = {} :: hsize2 = {} :: hsize3 = {}", hsize1, hsize2, hsize3);
      log.debug(
          "@ r = {} :: complen = {} :: ratio = {}",
          bitRate,
          hsize,
          (float) (num_pix) / (float) hsize);
    }
  }

  private static void verifyParamsOrThrow(@NonNull Bitmap bitmap, double bitRate) {
    if (bitmap.getPpi() <= 100) {
      throw new Wsq4jInvalidArgumentException("PPI is too low " + (bitmap.getPpi()));
    }

    if (bitmap.getDepth() != 8) {
      throw new Wsq4jInvalidArgumentException("DEPTH not implemented " + bitmap.getDepth());
    }

    if (bitmap.getLossyflag() != 0 && bitmap.getLossyflag() != 1) {
      throw new Wsq4jInvalidArgumentException(
          "LOSSY FLAG not implemented " + bitmap.getLossyflag());
    }

    if (bitRate < 0 || bitRate > 999) {
      throw new Wsq4jInvalidArgumentException("BITRATE not valid: " + bitRate);
    }
  }

  private static void debugHuffmanCodes(WSQHelper.HuffCode[] table) {

    if (!log.isDebugEnabled()) return;

    log.debug("Sym  Size  Code");

    for (int sym = 0; sym < table.length; sym++) {

      WSQHelper.HuffCode hc = table[sym];

      if (hc.size == 0) continue;

      @SuppressWarnings("StringConcatenationInFormatCall")
      String code =
          String.format("%" + hc.size + "s", Integer.toBinaryString(hc.code)).replace(' ', '0');

      String msg = String.format("%3s %4s %s", sym, hc.size, code);
      log.debug(msg);
    }
  }

  private static void debug_huffbits(WSQHelper.Ref<int[]> huffbits) {
    if (log.isDebugEnabled()) {
      int sum = 0;
      for (int v : huffbits.value) sum += v;

      log.debug("debug_huffbits WRITE DHT sum: {}", sum);
    }
  }

  private static void debug_qdata(int[] qdata) {
    if (log.isDebugEnabled()) {
      Set<Integer> values = new HashSet<>();

      int min = Integer.MAX_VALUE;
      int max = Integer.MIN_VALUE;

      for (int v : qdata) {
        values.add(v);
        min = Math.min(min, v);
        max = Math.max(max, v);
      }

      log.debug("qdata qsize={} distinct={} min={} max={}", qdata.length, values.size(), min, max);
    }
  }

  private static float[] conv_img_2_flt(
      byte[] data, WSQHelper.Ref<Float> m_shift, WSQHelper.Ref<Float> r_scale) {
    if (data == null) {
      throw new Wsq4jInvalidArgumentException("Image cannot be null");
    }
    int cnt; /* pixel cnt */
    int sum; /* sum of pixel values */
    int low, high; /* low/high pixel values */
    float low_diff, high_diff; /* new low/high pixels values shifting */

    float[] fip = new float[data.length];

    sum = 0;
    low = 255;
    high = 0;
    for (cnt = 0; cnt < data.length; cnt++) {
      if ((data[cnt] & 0xFF) > high) {
        high = data[cnt] & 0xFF;
      }
      if ((data[cnt] & 0xFF) < low) {
        low = data[cnt] & 0xFF;
      }
      sum += (data[cnt] & 0xFF);
    }

    m_shift.value = (float) sum / (float) data.length; // mean

    low_diff = m_shift.value - low;
    high_diff = high - m_shift.value;

    //noinspection ManualMinMaxCalculation
    if (low_diff >= high_diff) {
      r_scale.value = low_diff;
    } else {
      r_scale.value = high_diff;
    }

    r_scale.value /= (float) 128.0;

    for (cnt = 0; cnt < data.length; cnt++) {
      fip[cnt] = ((float) (data[cnt] & 0xFF) - m_shift.value) / r_scale.value;
    }
    return fip;
  }

  /** WSQ decompose the image. NOTE: this routine modifies and returns the results in "fdata" */
  @SuppressWarnings("SameParameterValue")
  private static void wsqDecompose(
      WSQHelper.Token token,
      float[] fdata,
      int width,
      int height,
      float[] hifilt,
      int hisz,
      float[] lofilt,
      int losz) {

    int num_pix = width * height;
    /* Allocate temporary floating point pixmap. */
    float[] fdata1 = new float[num_pix];

    /* Compute the Wavelet image decomposition. */
    for (int node = 0; node < token.wtree.length; node++) {
      int fdataBse = (token.wtree[node].y * width) + token.wtree[node].x;

      getLets(
          fdata1,
          fdata,
          0,
          fdataBse,
          token.wtree[node].leny,
          token.wtree[node].lenx,
          width,
          1,
          hifilt,
          hisz,
          lofilt,
          losz,
          token.wtree[node].invrw);
      getLets(
          fdata,
          fdata1,
          fdataBse,
          0,
          token.wtree[node].lenx,
          token.wtree[node].leny,
          1,
          width,
          hifilt,
          hisz,
          lofilt,
          losz,
          token.wtree[node].invcl);
    }
  }

  private static void getLets(
      float[] newdata, /* image pointers for creating subband splits */
      float[] olddata,
      int newIndex,
      int oldIndex,
      int len1, /* temporary length parameters */
      int len2,
      int pitch, /* pitch gives next row_col to filter */
      int stride, /*           stride gives next pixel to filter */
      float[] hi,
      int hsz, /* NEW */
      float[] lo, /* filter coefficients */
      int lsz, /* NEW */
      int inv) /* spectral inversion? */ {
    if (newdata == null) {
      throw new Wsq4jInvalidArgumentException("newdata == null");
    }
    if (olddata == null) {
      throw new Wsq4jInvalidArgumentException("olddata == null");
    }
    if (lo == null) {
      throw new Wsq4jInvalidArgumentException("lo == null");
    }

    int lopass, hipass; /* pointers of where to put lopass
                                   and hipass filter outputs */
    int p0, p1; /* pointers to image pixels used */
    int pix, rw_cl; /* pixel counter and row/column counter */
    int i, da_ev; /* even or odd row/column of pixels */
    int fi_ev;
    int loc, hoc, nstr, pstr;
    int llen, hlen;
    int lpxstr, lspxstr;
    int lpx, lspx;
    int hpxstr, hspxstr;
    int hpx, hspx;
    int olle, ohle;
    int olre, ohre;
    int lle, lle2;
    int lre, lre2;
    int hle, hle2;
    int hre, hre2;

    da_ev = len2 % 2;
    fi_ev = lsz % 2;

    if (fi_ev != 0) {
      loc = (lsz - 1) / 2;
      hoc = (hsz - 1) / 2 - 1;
      olle = 0;
      ohle = 0;
      olre = 0;
      ohre = 0;
    } else {
      loc = lsz / 2 - 2;
      hoc = hsz / 2 - 2;
      olle = 1;
      ohle = 1;
      olre = 1;
      ohre = 1;

      if (loc == -1) {
        loc = 0;
        olle = 0;
      }
      if (hoc == -1) {
        hoc = 0;
        ohle = 0;
      }

      for (i = 0; i < hsz; i++) {
        hi[i] *= (float) -1.0;
      }
    }

    pstr = stride;
    nstr = -pstr;

    if (da_ev != 0) {
      llen = (len2 + 1) / 2;
      hlen = llen - 1;
    } else {
      llen = len2 / 2;
      hlen = llen;
    }

    for (rw_cl = 0; rw_cl < len1; rw_cl++) {
      if (inv != 0) {
        hipass = newIndex + rw_cl * pitch;
        lopass = hipass + hlen * stride;
      } else {
        lopass = newIndex + rw_cl * pitch;
        hipass = lopass + llen * stride;
      }

      p0 = oldIndex + rw_cl * pitch;
      p1 = p0 + (len2 - 1) * stride;

      lspx = p0 + (loc * stride);
      lspxstr = nstr;
      lle2 = olle;
      lre2 = olre;
      hspx = p0 + (hoc * stride);
      hspxstr = nstr;
      hle2 = ohle;
      hre2 = ohre;
      for (pix = 0; pix < hlen; pix++) {
        lpxstr = lspxstr;
        lpx = lspx;
        lle = lle2;
        lre = lre2;
        newdata[lopass] = olddata[lpx] * lo[0];
        for (i = 1; i < lsz; i++) {
          if (lpx == p0) {
            if (lle != 0) {
              lpxstr = 0;
              lle = 0;
            } else {
              lpxstr = pstr;
            }
          }
          if (lpx == p1) {
            if (lre != 0) {
              lpxstr = 0;
              lre = 0;
            } else {
              lpxstr = nstr;
            }
          }
          lpx += lpxstr;
          newdata[lopass] += olddata[lpx] * lo[i];
        }
        lopass += stride;

        hpxstr = hspxstr;
        hpx = hspx;
        hle = hle2;
        hre = hre2;
        newdata[hipass] = olddata[hpx] * hi[0];
        for (i = 1; i < hsz; i++) {
          if (hpx == p0) {
            if (hle != 0) {
              hpxstr = 0;
              hle = 0;
            } else {
              hpxstr = pstr;
            }
          }
          if (hpx == p1) {
            if (hre != 0) {
              hpxstr = 0;
              hre = 0;
            } else {
              hpxstr = nstr;
            }
          }
          hpx += hpxstr;
          newdata[hipass] += olddata[hpx] * hi[i];
        }
        hipass += stride;

        for (i = 0; i < 2; i++) {
          if (lspx == p0) {
            if (lle2 != 0) {
              lspxstr = 0;
              lle2 = 0;
            } else {
              lspxstr = pstr;
            }
          }
          lspx += lspxstr;
          if (hspx == p0) {
            if (hle2 != 0) {
              hspxstr = 0;
              hle2 = 0;
            } else {
              hspxstr = pstr;
            }
          }
          hspx += hspxstr;
        }
      }
      if (da_ev != 0) {
        lpxstr = lspxstr;
        lpx = lspx;
        lle = lle2;
        lre = lre2;
        newdata[lopass] = olddata[lpx] * lo[0];
        for (i = 1; i < lsz; i++) {
          if (lpx == p0) {
            if (lle != 0) {
              lpxstr = 0;
              lle = 0;
            } else {
              lpxstr = pstr;
            }
          }
          if (lpx == p1) {
            if (lre != 0) {
              lpxstr = 0;
              lre = 0;
            } else {
              lpxstr = nstr;
            }
          }
          lpx += lpxstr;
          newdata[lopass] += olddata[lpx] * lo[i];
        }
        //noinspection UnusedAssignment
        lopass += stride;
      }
    }
    if (fi_ev == 0) {
      for (i = 0; i < hsz; i++) {
        hi[i] *= (float) -1.0;
      }
    }
  }

  /**
   * /* This routine calculates the variances of the subbands.
   *
   * @param token contains quant_vals quantization parameters and quantization "tree" and treelen.
   *     NOTE: This routine will write to <code>var</code> field inside <code>quant_vals</code>.
   * @param fip image pointer
   * @param width image width
   * @param height image height
   */
  private static void variance(
      WSQHelper.Token token, float[] fip, int width, @SuppressWarnings("unused") int height) {
    int fp; /* temp image pointer */
    int lenx, leny; /* dimensions of area to calculate variance */
    int skipx, skipy; /* pixels to skip to get to area for variance calculation */
    int row, col; /* dimension counters */
    float ssq; /* sum of squares */
    float sum2; /* variance calculation parameter */
    float sum_pix; /* sum of pixels */
    float vsum; /* variance sum for subbands 0-3 */

    vsum = 0;
    for (int cvr = 0; cvr < 4; cvr++) {
      fp = ((token.qtree[cvr].y) * width) + token.qtree[cvr].x;
      ssq = 0.0f;
      sum_pix = 0.0f;

      skipx = token.qtree[cvr].lenx / 8;
      skipy = (9 * token.qtree[cvr].leny) / 32;

      lenx = (3 * token.qtree[cvr].lenx) / 4;
      leny = (7 * token.qtree[cvr].leny) / 16;

      fp += (skipy * width) + skipx;
      for (row = 0; row < leny; row++, fp += (width - lenx)) {
        for (col = 0; col < lenx; col++) {
          sum_pix += fip[fp];
          ssq += fip[fp] * fip[fp];
          fp++;
        }
      }
      sum2 = (sum_pix * sum_pix) / (lenx * leny);
      token.quant_vals.var[cvr] = (ssq - sum2) / ((lenx * leny) - 1.0f);
      vsum += token.quant_vals.var[cvr];
    }

    // This part is needed to comply with WSQ 3.1
    if (vsum < 20000.0) {
      for (int cvr = 0; cvr < NUM_SUBBANDS; cvr++) {
        fp = (token.qtree[cvr].y * width) + token.qtree[cvr].x;
        ssq = 0;
        sum_pix = 0;

        lenx = token.qtree[cvr].lenx;
        leny = token.qtree[cvr].leny;

        for (row = 0; row < leny; row++, fp += (width - lenx)) {
          for (col = 0; col < lenx; col++) {
            sum_pix += fip[fp];
            ssq += fip[fp] * fip[fp];
            fp++;
          }
        }
        sum2 = (sum_pix * sum_pix) / (lenx * leny);
        token.quant_vals.var[cvr] = (float) ((ssq - sum2) / ((lenx * leny) - 1.0));
      }
    } else {
      for (int cvr = 4; cvr < NUM_SUBBANDS; cvr++) {
        fp = (token.qtree[cvr].y * width) + token.qtree[cvr].x;
        ssq = 0;
        sum_pix = 0;

        skipx = token.qtree[cvr].lenx / 8;
        skipy = (9 * token.qtree[cvr].leny) / 32;

        lenx = (3 * token.qtree[cvr].lenx) / 4;
        leny = (7 * token.qtree[cvr].leny) / 16;

        fp += (skipy * width) + skipx;
        for (row = 0; row < leny; row++, fp += (width - lenx)) {
          for (col = 0; col < lenx; col++) {
            sum_pix += fip[fp];
            ssq += fip[fp] * fip[fp];
            fp++;
          }
        }
        sum2 = (sum_pix * sum_pix) / (lenx * leny);
        token.quant_vals.var[cvr] = (float) ((ssq - sum2) / ((lenx * leny) - 1.0));
      }
    }
  }

  /**
   * This routine quantizes the wavelet subbands.
   *
   * @param token contains quantization parameters, quantization tree, size of quantization tree
   * @param qsize output size
   * @param fip floating point image pointer
   * @param width image width
   * @param height image height
   * @return quantized image
   */
  private static int[] quantize(
      WSQHelper.Token token, WSQHelper.Ref<Integer> qsize, float[] fip, int width, int height) {
    int row, col; /* temp image characteristic parameters */
    float zbin; /* zero bin size */
    float[] A = new float[NUM_SUBBANDS]; /* subband "weights" for quantization */
    float[] m = new float[NUM_SUBBANDS]; /* subband size to image size ratios */
    /* (reciprocal of FBI spec for 'm')  */
    float m1, m2, m3; /* reciprocal constants for 'm' */
    float[] sigma = new float[NUM_SUBBANDS]; /* square root of subband variances */
    int[] K0 = new int[NUM_SUBBANDS]; /* initial list of subbands w/variance >= thresh */
    int[] K1 = new int[NUM_SUBBANDS]; /* working list of subbands */
    int K, nK; /* pointers to sets of subbands */
    boolean[] NP; /* current subbounds with nonpositive bit rates. */
    int K0len; /* number of subbands in K0 */
    int Klen, nKlen; /* number of subbands in other subband lists */
    int NPlen; /* number of subbands flagged in NP */
    float S; /* current frac of subbands w/positive bit rate */
    float q; /* current proportionality constant */
    float P; /* product of 'q/Q' ratios */

    /* Set up 'A' table. */
    for (int cnt = 0; cnt < STRT_SUBBAND_3; cnt++) {
      A[cnt] = 1.0f;
    }
    // cf spec 3.1 §3.2
    A[STRT_SUBBAND_3 /*52*/] = 1.32f;
    A[STRT_SUBBAND_3 + 1 /*53*/] = 1.08f;
    A[STRT_SUBBAND_3 + 2 /*54*/] = 1.42f;
    A[STRT_SUBBAND_3 + 3 /*55*/] = 1.08f;
    A[STRT_SUBBAND_3 + 4 /*56*/] = 1.32f;
    A[STRT_SUBBAND_3 + 5 /*57*/] = 1.42f;
    A[STRT_SUBBAND_3 + 6 /*58*/] = 1.08f;
    A[STRT_SUBBAND_3 + 7 /*59*/] = 1.08f;

    for (int cnt = 0; cnt < MAX_SUBBANDS; cnt++) {
      token.quant_vals.qbss[cnt] = 0.0f;
      token.quant_vals.qzbs[cnt] = 0.0f;
    }

    /* Set up 'Q1' (prime) table. */
    for (int cnt = 0; cnt < NUM_SUBBANDS; cnt++) {
      if (token.quant_vals.var[cnt] < VARIANCE_THRESH) {
        token.quant_vals.qbss[cnt] = 0.0f;
      } else {
        /* NOTE: q has been taken out of the denominator in the next */
        /*       2 formulas from the original code. */
        if (cnt < STRT_SIZE_REGION_2 /*4*/) {
          token.quant_vals.qbss[cnt] = 1.0f;
        } else {
          token.quant_vals.qbss[cnt] =
              10.0f / (A[cnt] * (float) Math.log(token.quant_vals.var[cnt]));
        }
      }
    }

    /* Set up output buffer. */
    int[] sip = new int[width * height];
    int sptr = 0;

    /* Set up 'm' table (these values are the reciprocal of 'm' in the FBI spec). */
    m1 = 1.0f / 1024.0f;
    m2 = 1.0f / 256.0f;
    m3 = 1.0f / 16.0f;
    for (int cnt = 0; cnt < STRT_SIZE_REGION_2; cnt++) {
      m[cnt] = m1;
    }
    for (int cnt = STRT_SIZE_REGION_2; cnt < STRT_SIZE_REGION_3; cnt++) {
      m[cnt] = m2;
    }
    for (int cnt = STRT_SIZE_REGION_3; cnt < NUM_SUBBANDS; cnt++) {
      m[cnt] = m3;
    }

    /* Initialize 'K0' and 'K1' lists. */
    K0len = 0;
    for (int cnt = 0; cnt < NUM_SUBBANDS; cnt++) {
      if (token.quant_vals.var[cnt] >= VARIANCE_THRESH) {
        K0[K0len] = cnt;
        K1[K0len++] = cnt;
        /* Compute square root of subband variance. */
        sigma[cnt] = (float) Math.sqrt(token.quant_vals.var[cnt]);
      }
    }
    K = 0;
    Klen = K0len;

    while (true) {
      /* Compute new 'S' */
      S = 0.0f;
      for (int i = 0; i < Klen; i++) {
        /* Remember 'm' is the reciprocal of spec. */
        S += m[K1[K + i]];
      }

      /* Compute product 'P' */
      P = 1.0f;
      for (int i = 0; i < Klen; i++) {
        /* Remember 'm' is the reciprocal of spec. */
        P *= (float) Math.pow((sigma[K1[K + i]] / token.quant_vals.qbss[K1[K + i]]), m[K1[K + i]]);
      }

      /* Compute new 'q' */
      q =
          ((float) Math.pow(2, ((token.quant_vals.r / S) - 1.0f)) / 2.5f)
              / (float) Math.pow(P, (1.0f / S));

      /* Flag subbands with non-positive bitrate. */
      NP = new boolean[NUM_SUBBANDS];
      NPlen = 0;
      for (int i = 0; i < Klen; i++) {
        if ((token.quant_vals.qbss[K1[K + i]] / q) >= (5.0 * sigma[K1[K + i]])) {
          NP[K1[K + i]] = true;
          NPlen++;
        }
      }

      /* If list of subbands with non-positive bitrate is empty ... */
      if (NPlen == 0) {
        /* Then we are done, so break from while loop. */
        break;
      }

      /* Assign new subband set to previous set K minus subbands in set NP. */
      nK = 0;
      nKlen = 0;
      for (int i = 0; i < Klen; i++) {
        if (!NP[K1[K + i]]) {
          K1[nK + nKlen++] = K1[K + i];
        }
      }

      /* Assign new set as K. */
      //noinspection DataFlowIssue
      K = nK;
      Klen = nKlen;
    }

    /* Flag subbands that are in set 'K0' (the very first set). */
    nK = 0;
    Arrays.fill(K1, nK, NUM_SUBBANDS, 0); // was: memset(nK, 0, NUM_SUBBANDS * sizeof(int));
    for (int i = 0; i < K0len; i++) {
      K1[nK + K0[i]] = 1; /* MO: was = TRUE */
    }
    /* Set 'Q' values. */
    for (int cnt = 0; cnt < NUM_SUBBANDS; cnt++) {
      if (K1[nK + cnt] != 0) {
        token.quant_vals.qbss[cnt] /= q;
      } else {
        token.quant_vals.qbss[cnt] = 0.0f;
      }
      token.quant_vals.qzbs[cnt] = 1.2f * token.quant_vals.qbss[cnt];
    }

    /* Now ready to compute and store bin widths for subbands. */
    for (int cnt = 0; cnt < NUM_SUBBANDS; cnt++) {
      int fptr = (token.qtree[cnt].y * width) + token.qtree[cnt].x;

      if (token.quant_vals.qbss[cnt] != 0.0f) {

        zbin = token.quant_vals.qzbs[cnt] / 2.0f;

        for (row = 0; row < token.qtree[cnt].leny; row++, fptr += width - token.qtree[cnt].lenx) {
          for (col = 0; col < token.qtree[cnt].lenx; col++) {
            if (-zbin <= fip[fptr] && fip[fptr] <= zbin) {
              sip[sptr] = 0;
            } else if (fip[fptr] > 0.0f) {
              sip[sptr] = (int) (((fip[fptr] - zbin) / token.quant_vals.qbss[cnt]) + 1.0f);
            } else {
              sip[sptr] = (int) (((fip[fptr] + zbin) / token.quant_vals.qbss[cnt]) - 1.0f);
            }
            sptr++;
            fptr++;
          }
        }
      }
    }

    qsize.value = sptr;

    return sip;
  }

  /* Compute quantized WSQ subband block sizes.                           */
  private static void quant_block_sizes(
      WSQHelper.Token token,
      WSQHelper.Ref<Integer> oqsize1,
      WSQHelper.Ref<Integer> oqsize2,
      WSQHelper.Ref<Integer> oqsize3) {
    int qsize1, qsize2, qsize3;
    int node;

    /* Compute temporary sizes of 3 WSQ subband blocks. */
    qsize1 = token.wtree[14].lenx * token.wtree[14].leny;
    qsize2 =
        (token.wtree[5].leny * token.wtree[1].lenx) + (token.wtree[4].lenx * token.wtree[4].leny);
    qsize3 =
        (token.wtree[2].lenx * token.wtree[2].leny) + (token.wtree[3].lenx * token.wtree[3].leny);

    /* Adjust size of quantized WSQ subband blocks. */
    for (node = 0; node < STRT_SUBBAND_2; node++) {
      if (token.quant_vals.qbss[node] == 0.0f) {
        qsize1 -= (token.qtree[node].lenx * token.qtree[node].leny);
      }
    }

    for (node = STRT_SUBBAND_2; node < STRT_SUBBAND_3; node++) {
      if (token.quant_vals.qbss[node] == 0.0f) {
        qsize2 -= (token.qtree[node].lenx * token.qtree[node].leny);
      }
    }

    for (node = STRT_SUBBAND_3; node < STRT_SUBBAND_DEL; node++) {
      if (token.quant_vals.qbss[node] == 0.0f) {
        qsize3 -= (token.qtree[node].lenx * token.qtree[node].leny);
      }
    }

    oqsize1.value = qsize1;
    oqsize2.value = qsize2;
    oqsize3.value = qsize3;
  }

  private static int putc_huffman_table(
      DataOutput dataOutput, int tableId, int[] huffbits, int[] huffvalues) throws IOException {
    int sum = 0;
    log.debug("Start writing huffman table.");
    /* DHT */
    dataOutput.writeShort(DHT_WSQ);
    sum += 2;

    /* "value(2) + table id(1) + bits(16)" */
    int table_len = 3 + MAX_HUFFBITS;
    int values_offset = table_len;
    for (int i = 0; i < MAX_HUFFBITS; i++) {
      table_len += huffbits[i]; /* values size */
    }

    if (log.isDebugEnabled()) {
      log.debug("Table Len = {}", table_len);
      log.debug("Table ID = {}", tableId);
      for (int i = 0; i < MAX_HUFFBITS; i++) {
        log.debug("bits[{}] = {}", i, huffbits[i]);
      }
      for (int i = 0; i < table_len - values_offset; i++) {
        log.debug("values[{}] = {}", i, huffvalues[i]);
      }
    }

    /* Table Len */
    dataOutput.writeShort(table_len & 0xFFFF);
    sum += 2;

    /* Table ID */
    dataOutput.writeByte(tableId & 0xFF);
    sum += 1;

    /* Huffbits (MAX_HUFFBITS) */
    for (int i = 0; i < MAX_HUFFBITS; i++) {
      dataOutput.writeByte(huffbits[i] & 0xFF);
      sum += 1;
    }

    /* Huffvalues (MAX_HUFFCOUNTS) */
    for (int i = 0; i < table_len - values_offset; i++) {
      dataOutput.writeByte(huffvalues[i] & 0xFF);
      sum += 1;
    }
    log.debug("Finished writing huffman table.\n");
    return sum;
  }

  private static int putc_frame_header_wsq(
      DataOutput dataOutput, int width, int height, float m_shift, float r_scale)
      throws IOException {
    int sum = 0;
    float flt_tmp; /* temp variable */
    byte scale_ex; /* exponent scaling parameter */

    int shrt_dat; /* temp variable */
    dataOutput.writeShort(SOF_WSQ); /* +2 = 2 */
    sum += 2;

    /* size of frame header */
    dataOutput.writeShort(17);
    sum += 2;
    /* black pixel */
    dataOutput.writeByte(0); /* +1 = 3 */
    sum += 1;
    /* white pixel */
    dataOutput.writeByte(255); /* +1 = 4 */
    sum += 1;
    dataOutput.writeShort(height); /* +2 = 5 */
    sum += 2;
    dataOutput.writeShort(width); /* +2 = 7 */
    sum += 2;

    flt_tmp = m_shift;
    scale_ex = 0;
    if (flt_tmp != 0.0) {
      while (flt_tmp < 65535) {
        scale_ex += 1;
        flt_tmp *= 10;
      }
      scale_ex -= 1;
      shrt_dat = Math.round(flt_tmp / 10.0f) & 0xFFFF;
    } else {
      shrt_dat = 0;
    }
    dataOutput.writeByte(scale_ex); /* +1 = 9 */
    sum += 1;
    dataOutput.writeShort(shrt_dat); /* +2 = 11 */
    sum += 2;

    flt_tmp = r_scale;
    scale_ex = 0;
    if (flt_tmp != 0.0) {
      while (flt_tmp < 65535) {
        scale_ex += 1;
        flt_tmp *= 10;
      }
      scale_ex -= 1;
      shrt_dat = Math.round(flt_tmp / 10.0f);
    } else {
      shrt_dat = 0;
    }
    dataOutput.writeByte(scale_ex); /* +1 = 12 */
    sum += 1;
    dataOutput.writeShort(shrt_dat); /* +2 = 13 */
    sum += 2;
    dataOutput.writeByte(EV_VALUE); /* +1 = 15 */
    sum += 1;
    dataOutput.writeShort(Wsq4jConfig.WSQ_SF_ID); /* +2 = 17 */
    sum += 2;
    return sum;
  }

  @SuppressWarnings("SameParameterValue")
  private static void putc_transform_table(
      DataOutput dataOutput, float[] lofilt, int losz, float[] hifilt, int hisz)
      throws IOException {
    long coef; /* filter coefficient indicator */
    long int_dat; /* temp variable */
    double dbl_tmp; /* temp variable */
    byte scale_ex;
    int sign; /* exponent scaling and sign parameters */

    if (losz < 0 || losz > MAX_LOFILT) {
      throw new Wsq4jEncodeException(
          "Writing transform table: losz out of range (max " + MAX_LOFILT + ")");
    }
    if (hisz < 0 || hisz > MAX_HIFILT) {
      throw new Wsq4jEncodeException(
          "Writing transform table: hisz out of range (max " + MAX_HIFILT + ")");
    }
    dataOutput.writeShort(DTT_WSQ);

    /* table size */
    dataOutput.writeShort(58);

    /* number analysis lowpass coefficients */
    dataOutput.writeByte(losz);

    /* number analysis highpass coefficients */
    dataOutput.writeByte(hisz);

    for (coef = (losz >> 1); (coef & 0xFFFFFFFFL) < (long) losz; coef++) {
      dbl_tmp = lofilt[(int) (coef & 0xFFFFFFFFL)];
      if (dbl_tmp >= 0.0) {
        sign = 0;
      } else {
        sign = 1;
        dbl_tmp *= -1.0;
      }
      scale_ex = 0;
      if (dbl_tmp == 0.0) {
        int_dat = 0;
      } else if (dbl_tmp < 4294967295.0) {
        while (dbl_tmp < 4294967295.0) {
          scale_ex += 1;
          dbl_tmp *= 10.0;
        }
        scale_ex -= 1;
        int_dat = Math.round(dbl_tmp / 10.0);
      } else {
        dbl_tmp = lofilt[(int) (coef & 0xFFFFFFFFL)];
        throw new Wsq4jEncodeException(
            String.format("putc_transform_table : lofilt[%d] to high at %f", coef, dbl_tmp));
      }

      dataOutput.writeByte(sign & 0xFF);
      dataOutput.writeByte(scale_ex);
      dataOutput.writeInt((int) (int_dat & 0xFFFFFFFFL));
    }

    for (coef = (hisz >> 1); (int) (coef & 0xFFFFFFFFL) < (long) hisz; coef++) {
      dbl_tmp = hifilt[(int) (coef & 0xFFFFFFFFL)];
      if (dbl_tmp >= 0.0) {
        sign = 0;
      } else {
        sign = 1;
        dbl_tmp *= -1.0;
      }
      scale_ex = 0;
      if (dbl_tmp == 0.0) {
        int_dat = 0;
      } else if (dbl_tmp < 4294967295.0) {
        while (dbl_tmp < 4294967295.0) {
          scale_ex += 1;
          dbl_tmp *= 10.0;
        }
        scale_ex -= 1;
        int_dat = (int) Math.round(dbl_tmp / 10.0);
      } else {
        dbl_tmp = hifilt[(int) (coef & 0xFFFFFFFFL)];
        throw new Wsq4jEncodeException(
            "putc_transform_table : hifilt[" + coef + "] to high at " + dbl_tmp);
      }
      dataOutput.writeByte(sign & 0xFF);
      dataOutput.writeByte(scale_ex & 0xFF);
      dataOutput.writeInt((int) (int_dat & 0xFFFFFFFFL));
    }
  }

  /** Stores quantization table in the output buffer. */
  // Check changes
  private static void putc_quantization_table(
      DataOutput dataOutput, WSQHelper.Token token) /* quantization parameters  */
      throws IOException {
    byte scale_ex, scale_ex2; /* exponent scaling parameters */
    int shrt_dat, shrt_dat2; /* temp variables */
    float flt_tmp; /* temp variable */

    dataOutput.writeShort(DQT_WSQ);

    /* table size */
    dataOutput.writeShort(389);

    /* exponent scaling value */
    dataOutput.writeByte(2);

    /* quantizer bin center parameter */
    dataOutput.writeShort(44);

    for (int sub = 0; sub < 64; sub++) {
      //noinspection ConstantValue
      if (sub >= 0 && sub < 60) {
        if (token.quant_vals.qbss[sub] != 0.0f) {
          flt_tmp = token.quant_vals.qbss[sub];
          scale_ex = 0;
          if (flt_tmp < 65535) {
            while (flt_tmp < 65535) {
              scale_ex += 1;
              flt_tmp *= 10;
            }
            scale_ex -= 1;
            shrt_dat = (int) Math.round(flt_tmp / 10.0);
          } else {
            flt_tmp = token.quant_vals.qbss[sub];
            throw new Wsq4jEncodeException(
                String.format("putc_quantization_table : Q[%d] to high at %f", sub, flt_tmp));
          }

          flt_tmp = token.quant_vals.qzbs[sub];
          scale_ex2 = 0;
          if (flt_tmp < 65535) {
            while (flt_tmp < 65535) {
              scale_ex2 += 1;
              flt_tmp *= 10;
            }
            scale_ex2 -= 1;
            shrt_dat2 = (int) Math.round(flt_tmp / 10.0);
          } else {
            flt_tmp = token.quant_vals.qzbs[sub];
            throw new Wsq4jInvalidArgumentException(
                String.format("putc_quantization_table : Z[%d] to high at %f", sub, flt_tmp));
          }
        } else {
          scale_ex = 0;
          scale_ex2 = 0;
          shrt_dat = 0;
          shrt_dat2 = 0;
        }
      } else {
        scale_ex = 0;
        scale_ex2 = 0;
        shrt_dat = 0;
        shrt_dat2 = 0;
      }

      dataOutput.writeByte(scale_ex);
      dataOutput.writeShort(shrt_dat);
      dataOutput.writeByte(scale_ex2);
      dataOutput.writeShort(shrt_dat2);
    }
  }

  /**
   * Stores block header to the output buffer.
   *
   * @param dataOutput token
   * @param table huffman table indicator
   */
  private static int putc_block_header(DataOutput dataOutput, int table) throws IOException {
    int sum = 0;
    dataOutput.writeShort(SOB_WSQ);
    sum += 2;

    /* block header size */
    dataOutput.writeShort(3);
    sum += 2;
    dataOutput.writeByte(table & 0xFF);
    sum += 1;
    return sum;
  }

  private static void putc_nistcom_wsq(
      DataOutput dataOutput,
      Bitmap bitmap,
      float r_bitrate,
      Map<String, String> metadata,
      String[] comments)
      throws IOException {
    Map<String, String> nistcom = new LinkedHashMap<>();
    // These attributes will be filled later
    nistcom.put(NCM_HEADER, "---");
    nistcom.put(NCM_PIX_WIDTH, "---");
    nistcom.put(NCM_PIX_HEIGHT, "---");
    nistcom.put(NCM_PIX_DEPTH, "---");
    nistcom.put(NCM_PPI, "---");
    nistcom.put(NCM_LOSSY, "---");
    nistcom.put(NCM_COLORSPACE, "---");
    nistcom.put(NCM_COMPRESSION, "---");
    nistcom.put(NCM_WSQ_RATE, "---");

    if (metadata != null) {
      nistcom.putAll(metadata);
    }

    nistcom.put(NCM_HEADER, Integer.toString(nistcom.size()));
    nistcom.put(NCM_PIX_WIDTH, Integer.toString(bitmap.getWidth()));
    nistcom.put(NCM_PIX_HEIGHT, Integer.toString(bitmap.getHeight()));
    nistcom.put(NCM_PPI, Integer.toString(bitmap.getPpi()));
    nistcom.put(NCM_PIX_DEPTH, "8"); // WSQ has always 8 bpp
    nistcom.put(NCM_LOSSY, "1"); // WSQ is always lossy
    nistcom.put(NCM_COLORSPACE, "GRAY");
    nistcom.put(NCM_COMPRESSION, "WSQ");
    nistcom.put(NCM_WSQ_RATE, Float.toString(r_bitrate));

    putc_comment(dataOutput, COM_WSQ, fetToString(nistcom));
    if (comments != null && comments.length > 0 && comments[0] != null) {
      for (String s : comments) {
        if (s != null) {
          putc_comment(dataOutput, COM_WSQ, s);
        }
      }
    } else {
      // avoid empty comments
      putc_comment(dataOutput, COM_WSQ, NO_COMMENT);
    }
  }

  @SuppressWarnings("SameParameterValue")
  private static void putc_comment(DataOutput dataOutput, int marker, String comment)
      throws IOException {
    dataOutput.writeShort(marker);

    /* comment size */
    byte[] textBytes = comment.getBytes(StandardCharsets.UTF_8);
    int hdr_size = 2 + textBytes.length;
    dataOutput.writeShort(hdr_size & 0xFFFF);

    dataOutput.write(textBytes);
  }

  // Check not changes
  private static WSQHelper.HuffCode[] gen_hufftable_wsq(
      WSQHelper.Ref<int[]> ohuffbits,
      WSQHelper.Ref<int[]> ohuffvalues,
      int[] sip,
      int offset,
      int[] block_sizes) {
    int[] codesize; /* code sizes to use */
    WSQHelper.Ref<Integer> last_size = new WSQHelper.Ref<>(); /* last huffvalue */
    int[] huffbits; /* huffbits values */
    int[] huffvalues; /* huffvalues */
    int[] huffcounts; /* counts for each huffman category */
    int[] huffcounts2; /* counts for each huffman category */
    WSQHelper.HuffCode[] hufftable1, hufftable2; /* hufftables */

    huffcounts = count_block(sip, offset, block_sizes[0]);
    if (huffcounts.length == 0) {
      throw new Wsq4jEncodeException("huffcounts is empty");
    }

    for (int i = 1; i < block_sizes.length; i++) {
      huffcounts2 = count_block(sip, offset + block_sizes[i - 1], block_sizes[i]);

      for (int j = 0; j < MAX_HUFFCOUNTS_WSQ; j++) {
        huffcounts[j] += huffcounts2[j];
      }
    }

    codesize = find_huff_sizes(huffcounts, MAX_HUFFCOUNTS_WSQ);

    /* tells if codesize is greater than MAX_HUFFBITS */
    WSQHelper.Ref<Boolean> adjust = new WSQHelper.Ref<>();

    huffbits = find_num_huff_sizes(adjust, codesize, MAX_HUFFCOUNTS_WSQ);

    if (adjust.value) {
      sort_huffbits(huffbits);
    }

    huffvalues = sort_code_sizes(codesize, MAX_HUFFCOUNTS_WSQ);

    hufftable1 = build_huffsizes(last_size, huffbits, MAX_HUFFCOUNTS_WSQ);

    build_huffcodes(hufftable1);
    check_huffcodes_wsq(hufftable1, last_size.value);

    hufftable2 = build_huffcode_table(hufftable1, last_size.value, huffvalues);

    ohuffbits.value = huffbits;
    ohuffvalues.value = huffvalues;
    return hufftable2;
  }

  /* This routine counts the number of occurences of each category */
  /* in the huffman coding tables.                                 */
  /*****************************************************************/
  // Check and impact changes
  private static int[] count_block(
      int[] sip, /* quantized data */
      int sip_offset, /* offset into sip */
      int sip_siz /* size of block being compressed */) /* maximum zero runs */ {
    int[] counts; /* count for each huffman category */
    int loMaxCoeff; /* lower (negative) MaxCoeff limit */
    int pix; /* temp pixel pointer */
    int rcnt = 0, state; /* zero run count and if current pixel
	                             is in a zero run or just a coefficient */
    int cnt; /* pixel counter */

    if (MAX_HUFFCOEFF < 0 || MAX_HUFFCOEFF > 0xffff) {
      throw new Wsq4jEncodeException("compress_block : MaxCoeff out of range.");
    }
    if (MAX_HUFFZRUN < 0 || MAX_HUFFZRUN > 0xffff) {
      throw new Wsq4jEncodeException("compress_block : MaxZRun out of range.");
    }
    /* Ininitalize vector of counts to 0. */
    counts = new int[MAX_HUFFCOUNTS_WSQ + 1];
    /* Set last count to 1. */
    counts[MAX_HUFFCOUNTS_WSQ] = 1;

    loMaxCoeff = 1 - MAX_HUFFCOEFF;
    state = COEFF_CODE;
    for (cnt = 0; cnt < sip_siz; cnt++) {
      if (cnt + sip_offset >= sip.length) {
        throw new Wsq4jEncodeException("sip_offset too large for sip.length");
      }
      pix = sip[cnt + sip_offset];
      switch (state) {
        case COEFF_CODE: /* for runs of zeros */
          if (pix == 0) {
            state = RUN_CODE;
            rcnt = 1;
            break;
          }
          if (pix > MAX_HUFFCOEFF) {
            if (pix > 255) {
              counts[103]++; /* 16bit pos esc */
            } else {
              counts[101]++; /* 8bit pos esc */
            }
          } else if (pix < loMaxCoeff) {
            if (pix < -255) {
              counts[104]++; /* 16bit neg esc */
            } else {
              counts[102]++; /* 8bit neg esc */
            }
          } else {
            counts[pix + 180]++; /* within table */
          }
          break;

        case RUN_CODE: /* get length of zero run */
          if (pix == 0 && rcnt < 0xFFFF) {
            ++rcnt;
            break;
          }
          /* limit rcnt to avoid EOF problem in bitio.c */
          if (rcnt <= MAX_HUFFZRUN) {
            counts[rcnt]++;
          } else if (rcnt <= 0xFF) {
            counts[105]++;
          } else if (rcnt <= 0xFFFF) {
            counts[106]++; /* 16bit zrun esc */
          } else {
            throw new Wsq4jEncodeException("count_block : Zrun to long in count block.");
          }

          if (pix != 0) {
            if (pix > MAX_HUFFCOEFF) {
              if (pix > 255) {
                counts[103]++; /* 16bit pos esc */
              } else {
                counts[101]++; /* 8bit pos esc */
              }
            } else if (pix < loMaxCoeff) {
              if (pix < -255) {
                counts[104]++; /* 16bit neg esc */
              } else {
                counts[102]++; /* 8bit neg esc */
              }
            } else {
              counts[pix + 180]++; /* within table */
            }
            state = COEFF_CODE;
          } else {
            rcnt = 1;
            //noinspection DataFlowIssue
            state = RUN_CODE;
          }
          break;
      }
    }
    if (state == RUN_CODE) {
      if (rcnt <= MAX_HUFFZRUN) {
        counts[rcnt]++;
      } else if (rcnt <= 0xFF) {
        counts[105]++;
      } else if (rcnt <= 0xFFFF) {
        counts[106]++; /* 16bit zrun esc */
      } else {
        throw new Wsq4jEncodeException("count_block : Zrun to long in count block.");
      }
    }

    return counts;
  }

  // Check changes
  @SuppressWarnings("SameParameterValue")
  private static int[] find_num_huff_sizes(
      WSQHelper.Ref<Boolean> adjust, int[] codesize, int max_huffcounts) {
    adjust.value = false;

    /* Allocate 2X desired number of bits due to possible codesize. */
    int[] bits = new int[2 * MAX_HUFFBITS];

    for (int i = 0; i < max_huffcounts; i++) {
      if (codesize[i] > 0 && codesize[i] <= bits.length) {
        bits[codesize[i] - 1]++;
      }
      if (codesize[i] > MAX_HUFFBITS) {
        adjust.value = true;
      }
    }
    return bits;
  }

  /** routine to sort the huffman code sizes */
  @SuppressWarnings("SameParameterValue")
  private static int[] sort_code_sizes(int[] codesize, int max_huffcount) {
    /* defines order of huffman codelengths in relation to the code sizes */
    int[] values = new int[max_huffcount + 1];
    int i2 = 0;
    for (int i = 1; i <= (MAX_HUFFBITS << 1); i++) {
      for (int i3 = 0; i3 < max_huffcount; i3++) {
        if (codesize[i3] == i) {
          values[i2] = i3;
          i2++;
        }
      }
    }
    return values;
  }

  /** This routine defines the huffman code sizes for each difference category */
  @SuppressWarnings("SameParameterValue")
  private static WSQHelper.HuffCode[] build_huffsizes(
      WSQHelper.Ref<Integer> temp_size, int[] huffbits, int max_huffcounts) {
    int number_of_codes = 1;

    WSQHelper.HuffCode[] huffcode_table = new WSQHelper.HuffCode[max_huffcounts + 1];
    for (int i = 0; i < huffcode_table.length; i++) {
      huffcode_table[i] = new WSQHelper.HuffCode();
    }

    temp_size.value = 0;

    for (int code_size = 1; code_size <= MAX_HUFFBITS; code_size++) {
      while (number_of_codes <= huffbits[code_size - 1]) {
        huffcode_table[temp_size.value].size = code_size;
        (temp_size.value)++;
        number_of_codes++;
      }
      number_of_codes = 1;
    }
    huffcode_table[temp_size.value].size = 0;
    return huffcode_table;
  }

  /**
   * Routine to optimize code sizes by frequency of difference values.
   */
  @SuppressWarnings("SameParameterValue")
  private static int[] find_huff_sizes(int[] freq, int max_huffcounts) {
    int value1;
    /* smallest and next smallest frequency */
    int value2; /* of difference occurrence in the largest difference category */

    /* codesizes for each category */
    int[] codesize = new int[max_huffcounts + 1];

    /* pointer used to generate codesizes */
    int[] others = new int[max_huffcounts + 1];

    for (int i = 0; i <= max_huffcounts; i++) {
      others[i] = -1;
    }

    while (true) {

      int[] values = find_least_freq(freq, MAX_HUFFCOUNTS_WSQ);
      value1 = values[0];
      value2 = values[1];

      if (value2 == -1) {
        break;
      }

      freq[value1] += freq[value2];
      freq[value2] = 0;

      codesize[value1]++;
      while (others[value1] != -1) {
        value1 = others[value1];
        codesize[value1]++;
      }
      others[value1] = value2;
      codesize[value2]++;

      while (others[value2] != -1) {
        value2 = others[value2];
        codesize[value2]++;
      }
    }

    return codesize;
  }

  /**
   * Routine to find the largest difference with the least frequency value
   */
  /*
   * FIXME
   * what happens if first two freqs found are smallest but in wrong order?
   * FIXME
   */
  @SuppressWarnings("SameParameterValue")
  private static int[] find_least_freq(int[] freq, int max_huffcounts) {
    int code_temp; /*store code*/
    int value_temp; /*store size*/
    int code2 = Integer.MAX_VALUE; /*next smallest frequency in largest diff category*/
    int code1 = Integer.MAX_VALUE; /*smallest frequency in largest difference category*/
    int set = 1; /*flag first two non-zero frequency values*/

    int value1 = -1;
    int value2 = -1;

    for (int i = 0; i <= max_huffcounts; i++) {
      if (freq[i] == 0) {
        continue;
      }
      if (set == 1) {
        code1 = freq[i];
        value1 = i;
        set++;
        continue;
      }
      if (set == 2) {
        code2 = freq[i];
        value2 = i;
        set++;
      }
      code_temp = freq[i];
      value_temp = i;
      if (code1 < code_temp && code2 < code_temp) {
        continue;
      }
      if ((code_temp < code1) || (code_temp == code1 && value_temp > value1)) {
        code2 = code1;
        value2 = value1;
        code1 = code_temp;
        value1 = value_temp;
        continue;
      }
      if ((code_temp < code2) || (code_temp == code2 && value_temp > value2)) {
        code2 = code_temp;
        value2 = value_temp;
      }
    }
    return new int[] {value1, value2};
  }

  /**
   * routine to insure that no huffman code size is greater than 16
   */
  private static void sort_huffbits(int[] bits) {
    int i, j;
    int l1, l2, l3;

    l3 = MAX_HUFFBITS << 1; /* 32 */
    l1 = l3 - 1; /* 31 */
    l2 = MAX_HUFFBITS - 1; /* 15 */

    int[] tbits = new int[l3];

    for (i = 0; i < MAX_HUFFBITS << 1; i++) {
      tbits[i] = bits[i];
    }

    for (i = l1; i > l2; i--) {
      while (tbits[i] > 0) {
        j = i - 2;
        while (tbits[j] == 0) {
          j--;
        }
        tbits[i] -= 2;
        tbits[i - 1] += 1;
        tbits[j + 1] += 2;
        tbits[j] -= 1;
      }
      tbits[i] = 0;
    }

    while (tbits[i] == 0) {
      i--;
    }

    tbits[i] -= 1;

    for (i = 0; i < MAX_HUFFBITS << 1; i++) {
      bits[i] = tbits[i];
    }

    for (i = MAX_HUFFBITS; i < l3; i++) {
      if (bits[i] > 0) {
        throw new Wsq4jEncodeException(
            "sort_huffbits : Code length is greater than 16 :" + bits[i]);
      }
    }
    if (log.isDebugEnabled()) {
      log.debug("Huffbits after sorting.");
      for (i = 0; i < MAX_HUFFBITS << 1; i++) log.debug("sort_bits[{}] = {}", i, bits[i]);
    }
  }

  /* This routine defines the huffman codes needed for each difference category */
  /******************************************************************************/
  private static void build_huffcodes(WSQHelper.HuffCode[] huffcode_table) {
    int pointer = 0; /*pointer to code word information*/
    int temp_code = 0; /*used to construct code word*/

    int temp_size = huffcode_table[0].size; /*used to construct code size*/

    if (huffcode_table[pointer].size == 0) {
      return;
    }

    do {
      do {
        huffcode_table[pointer].code = temp_code;
        temp_code = (temp_code + 1) & 0xFFFF;
        pointer++;
      } while (pointer < huffcode_table.length && huffcode_table[pointer].size == temp_size);

      if (pointer >= huffcode_table.length || huffcode_table[pointer].size == 0) {
        return;
      }

      do {
        temp_code = (temp_code << 1) & 0xFFFF;
        temp_size++;
      } while (huffcode_table[pointer].size != temp_size);
    } while (huffcode_table[pointer].size == temp_size);
  }

  private static void check_huffcodes_wsq(WSQHelper.HuffCode[] hufftable, int last_size) {
    boolean all_ones;

    for (int i = 0; i < last_size; i++) {
      all_ones = true;
      for (int k = 0; (k < hufftable[i].size) && all_ones; k++) {
        //noinspection ConstantValue
        all_ones = (all_ones && (((hufftable[i].code >> k) & 0x0001) != 0));
      }
      if (all_ones) {
        throw new Wsq4jEncodeException(
            "WARNING: A code in the hufftable contains an "
                + "all 1's code. This image may still be "
                + "decodable. It is not compliant with "
                + "the WSQ specification.");
      }
    }
  }

  /* routine to sort huffman codes and sizes */
  private static WSQHelper.HuffCode[] build_huffcode_table(
      WSQHelper.HuffCode[] in_huffcode_table, int last_size, int[] values) {
    WSQHelper.HuffCode[] new_huffcode_table = new WSQHelper.HuffCode[MAX_HUFFCOUNTS_WSQ + 1];
    for (int i = 0; i < new_huffcode_table.length; i++) {
      new_huffcode_table[i] = new WSQHelper.HuffCode();
    }

    for (int size = 0; size < last_size; size++) {
      int idx = values[size];
      if (idx < 0 || idx >= new_huffcode_table.length) continue;
      new_huffcode_table[idx].code = in_huffcode_table[size].code;
      new_huffcode_table[idx].size = in_huffcode_table[size].size;
    }

    return new_huffcode_table;
  }

  /* Routine "codes" the quantized image using the huffman tables. */
  @SuppressWarnings("SameParameterValue")
  private static byte[] compress_block(
      int[] sip, /* quantized image */
      int offset,
      int length,
      int max_huffcoeff,
      int max_huffzrun,
      WSQHelper.HuffCode[] codes) /* huffman code table  */
      throws IOException {
    log.debug(
        "compress_block parameters offset:{} length:{} size:{} max_huffcoeff:{} max_huffzrun:{}",
        offset,
        length,
        length - offset,
        max_huffcoeff,
        max_huffzrun);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    DataOutput dataOutput = new DataOutputStream(outputStream);
    int sum = 0;
    int LoMaxCoeff; /* lower (negative) MaxCoeff limit */
    int pix; /* temp pixel pointer */
    int rcnt = 0, state; /* zero run count and if current pixel
                             is in a zero run or just a coefficient */
    int cnt; /* pixel counter */

    if (max_huffcoeff < 0 || max_huffcoeff > 0xffff) {
      throw new Wsq4jEncodeException("compress_block : MaxCoeff out of range.");
    }
    if (max_huffzrun < 0 || max_huffzrun > 0xffff) {
      throw new Wsq4jEncodeException("compress_block : MaxZRun out of range.");
    }
    LoMaxCoeff = 1 - max_huffcoeff;

    WSQHelper.Ref<Integer> outbit = new WSQHelper.Ref<>(7);
    WSQHelper.Ref<Integer> bytes = new WSQHelper.Ref<>(0);
    WSQHelper.Ref<Integer> bits = new WSQHelper.Ref<>(0);

    int loopCount = 0;
    state = COEFF_CODE;
    // /!\ be carefully add +length
    for (cnt = offset; cnt < offset + length; cnt++, loopCount++) {
      pix = sip[cnt];

      if (log.isDebugEnabled() && cnt - offset < 20) {
        log.debug("pix [{}]", pix);
      }

      switch (state) {
        case COEFF_CODE:
          if (pix == 0) {
            state = RUN_CODE;
            rcnt = 1;
            break;
          }
          if (pix > max_huffcoeff) {
            if (pix > 255) {
              /* 16bit pos esc */
              sum += write_bits(dataOutput, codes[103].size, codes[103].code, outbit, bits, bytes);
              sum += write_bits(dataOutput, 16, pix, outbit, bits, bytes);
            } else {
              /* 8bit pos esc */
              sum += write_bits(dataOutput, codes[101].size, codes[101].code, outbit, bits, bytes);
              sum += write_bits(dataOutput, 8, pix, outbit, bits, bytes);
            }
          } else if (pix < LoMaxCoeff) {
            if (pix < -255) {
              /* 16bit neg esc */
              sum += write_bits(dataOutput, codes[104].size, codes[104].code, outbit, bits, bytes);
              sum += write_bits(dataOutput, 16, -(pix), outbit, bits, bytes);
            } else {
              /* 8bit neg esc */
              sum += write_bits(dataOutput, codes[102].size, codes[102].code, outbit, bits, bytes);
              sum += write_bits(dataOutput, 8, -(pix), outbit, bits, bytes);
            }
          } else {
            /* within table */
            sum +=
                write_bits(
                    dataOutput, codes[pix + 180].size, codes[pix + 180].code, outbit, bits, bytes);
          }
          break;

        case RUN_CODE:
          if (pix == 0 && rcnt < 0xFFFF) {
            ++rcnt;
            break;
          }
          if (rcnt <= max_huffzrun) {
            /* log zero run length */
            sum += write_bits(dataOutput, codes[rcnt].size, codes[rcnt].code, outbit, bits, bytes);
          } else if (rcnt <= 0xFF) {
            /* 8bit zrun esc */
            sum += write_bits(dataOutput, codes[105].size, codes[105].code, outbit, bits, bytes);
            sum += write_bits(dataOutput, 8, rcnt, outbit, bits, bytes);
          } else if (rcnt <= 0xFFFF) {
            /* 16bit zrun esc */
            sum += write_bits(dataOutput, codes[106].size, codes[106].code, outbit, bits, bytes);
            sum += write_bits(dataOutput, 16, rcnt, outbit, bits, bytes);
          } else {
            throw new Wsq4jEncodeException("compress_block : zrun too large.");
          }

          if (pix != 0) {
            if (pix > max_huffcoeff) {
              if (pix > 255) {
                /* 16bit pos esc */
                sum +=
                    write_bits(dataOutput, codes[103].size, codes[103].code, outbit, bits, bytes);
                sum += write_bits(dataOutput, 16, pix, outbit, bits, bytes);
              } else {
                /* 8bit pos esc */
                sum +=
                    write_bits(dataOutput, codes[101].size, codes[101].code, outbit, bits, bytes);
                sum += write_bits(dataOutput, 8, pix, outbit, bits, bytes);
              }
            } else if (pix < LoMaxCoeff) {
              if (pix < -255) {
                /* 16bit neg esc */
                sum +=
                    write_bits(dataOutput, codes[104].size, codes[104].code, outbit, bits, bytes);
                sum += write_bits(dataOutput, 16, -pix, outbit, bits, bytes);
              } else {
                /* 8bit neg esc */
                sum +=
                    write_bits(dataOutput, codes[102].size, codes[102].code, outbit, bits, bytes);
                sum += write_bits(dataOutput, 8, -pix, outbit, bits, bytes);
              }
            } else {
              /* within table */
              sum +=
                  write_bits(
                      dataOutput,
                      codes[pix + 180].size,
                      codes[pix + 180].code,
                      outbit,
                      bits,
                      bytes);
            }
            state = COEFF_CODE;
          } else {
            rcnt = 1;
            //noinspection DataFlowIssue
            state = RUN_CODE;
          }
          break;
      }
    }
    log.debug("loopCount: [{}]", loopCount);
    if (state == RUN_CODE) {
      if (rcnt <= max_huffzrun) {
        sum += write_bits(dataOutput, codes[rcnt].size, codes[rcnt].code, outbit, bits, bytes);
      } else if (rcnt <= 0xFF) {
        sum += write_bits(dataOutput, codes[105].size, codes[105].code, outbit, bits, bytes);
        sum += write_bits(dataOutput, 8, rcnt, outbit, bits, bytes);
      } else if (rcnt <= 0xFFFF) {
        sum += write_bits(dataOutput, codes[106].size, codes[106].code, outbit, bits, bytes);
        sum += write_bits(dataOutput, 16, rcnt, outbit, bits, bytes);
      } else {
        throw new Wsq4jEncodeException("compress_block : zrun2 too large.");
      }
    }

    flush_bits(dataOutput, outbit, bits, bytes);
    log.debug("compress_block wrote sum: {}", sum);

    return outputStream.toByteArray();
  }

  private static String fetToString(Map<String, String> fet) {
    StringBuilder result = new StringBuilder();
    Set<Map.Entry<String, String>> entries = fet.entrySet();
    for (Map.Entry<String, String> entry : entries) {
      if (entry.getKey() == null) {
        continue;
      }
      if (entry.getValue() == null) {
        continue;
      }
      try {
        String key = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name());
        String value = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name());
        result.append(key);
        result.append(" ");
        result.append(value);
        result.append("\n");
      } catch (UnsupportedEncodingException e) {
        throw new Wsq4jEncodeException(e);
      }
    }
    return result.toString();
  }

  /* Routine to write "compressed" bits to output buffer. */
  // Check no changes
  private static int write_bits(
      DataOutput outbuf,
      int size, /* numbers bits of code to write into buffer   */
      int code, /* info to write into buffer                   */
      WSQHelper.Ref<Integer> outbit, /* current bit location in out buffer byte     */
      WSQHelper.Ref<Integer> bits, /* byte to write to output buffer              */
      WSQHelper.Ref<Integer> bytes) /* count of number bytes written to the buffer */
      throws IOException {
    int num;
    num = size;

    for (--num; num >= 0; num--) {
      bits.value <<= 1;
      bits.value |= (((code >> num) & 0x0001)) & 0xFF;

      if (--(outbit.value) < 0) {
        outbuf.write(bits.value);
        if ((bits.value & 0xFF) == 0xFF) {
          outbuf.write(0);
          bytes.value++;
        }
        bytes.value++;
        outbit.value = 7;
        bits.value = 0;
      }
    }
    return size;
  }

  /* Routine to "flush" left over bits in last */
  /* byte after compressing a block.           */
  private static void flush_bits(
      DataOutput outbuf, /* output data buffer */
      WSQHelper.Ref<Integer> outbit, /* current bit location in out buffer byte */
      WSQHelper.Ref<Integer> bits, /* byte to write to output buffer */
      WSQHelper.Ref<Integer> bytes) /* count of number bytes written to the buffer */
      throws IOException {
    int cnt; /* temp counter */

    if (outbit.value != 7) {
      for (cnt = outbit.value; cnt >= 0; cnt--) {
        bits.value <<= 1;
        bits.value |= 0x01;
      }

      outbuf.write(bits.value);
      if (bits.value == 0xFF) {
        bits.value = 0;
        outbuf.write(0);
        bytes.value++;
      }
      bytes.value++;
      outbit.value = 7;
      bits.value = 0;
    }
  }
}
