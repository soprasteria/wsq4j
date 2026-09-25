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

import static io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder.WSQDecoder.*;

import io.github.soprasteria.wsq4j.domain.constants.NISTConstants;
import io.github.soprasteria.wsq4j.domain.constants.WSQConstants;
import io.github.soprasteria.wsq4j.domain.entities.Pair;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.imageio.ImageIO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WSQAnalyse implements WSQConstants, NISTConstants {

  public static void analyse(InputStream is, Path analyseDirPath, String filename)
      throws IOException {
    if (is instanceof DataInput) {
      analyse((DataInput) is, analyseDirPath, filename);
    } else {
      analyse((DataInput) new DataInputStream(is), analyseDirPath, filename);
    }
  }

  public static void analyse(DataInput dataInput, Path analyseDirPath, String filename)
      throws IOException {

    Pair<WSQHelper.Token, WSQHelper.HeaderFrm> decodePair =
        WSQDecoder.decodeTokenAndHeader(dataInput);
    WSQHelper.Token token = decodePair.getLeft();
    WSQHelper.HeaderFrm header = decodePair.getRight();

    /* Decode the Huffman encoded buffer blocks. */
    int[] qdata = huffman_decode_data_mem(dataInput, token, header.width * header.height);

    WSQCmpGenerator.generate(analyseDirPath, filename, qdata, token.qtree);
  }

  public static boolean compareFingerprints(WSQFingerprint fp1, WSQFingerprint fp2)
      throws IOException {
    return compareFingerprints(fp1, fp2, null);
  }

  public static boolean compareFingerprints(WSQFingerprint fp1, WSQFingerprint fp2, Writer writer)
      throws IOException {
    boolean sameHuffmanTable0 = fp1.huffmanTable0.equals(fp2.huffmanTable0);
    String report = String.format("Compare Huffman 0 : %b\n", sameHuffmanTable0);
    boolean samehuffmanTable1 = fp1.huffmanTable1.equals(fp2.huffmanTable1);
    report += String.format("Compare Huffman 1 : %b\n", samehuffmanTable1);
    boolean sameQuantizationTable = fp1.quantizationTable.equals(fp2.quantizationTable);
    report += String.format("Compare DQT       : %b\n", sameQuantizationTable);
    boolean sameQuantizationValues = fp1.quantizationValues.equals(fp2.quantizationValues);
    report += String.format("Compare Quant     : %b\n", sameQuantizationValues);
    boolean sameQuantTree = fp1.quantTree.equals(fp2.quantTree);
    report += String.format("Compare QTree     : %b\n", sameQuantTree);

    double compareHisto = histogramDistance(fp1.histogram, fp2.histogram);
    report +=
        String.format("Compare Histogram     : %b (%f)\n\n", compareHisto < 100, compareHisto);

    if (writer != null) {
      writer.write(report);
    }

    return sameHuffmanTable0
        && samehuffmanTable1
        && sameQuantizationTable
        && sameQuantizationValues
        && sameQuantTree;
  }

  public static double histogramDistance(Map<Integer, Integer> h1, Map<Integer, Integer> h2) {

    Set<Integer> values = new TreeSet<>();
    values.addAll(h1.keySet());
    values.addAll(h2.keySet());

    double diff = 0;
    for (Integer v : values) {
      int val1 = h1.getOrDefault(v, 0);
      int val2 = h2.getOrDefault(v, 0);

      diff += Math.abs(val1 - val2);
    }
    return diff;
  }

  @SuppressWarnings("unused")
  public static void showHistograms(WSQFingerprint fp1, WSQFingerprint fp2) throws IOException {
    showHistograms(fp1, fp2, null);
  }

  public static void showHistograms(WSQFingerprint fp1, WSQFingerprint fp2, Writer writer)
      throws IOException {

    double cumul1 = 100.0;
    double cumul2 = 100.0;

    String header =
        String.format(
            "%6s -> %10s %12s %12s    %10s %12s %12s    %6s\n",
            "i", "val1", "pct1", "cumul1", "val2", "pct2", "cumul2", "eq");
    if (writer != null) {
      writer.write(header);
    } else {
      System.out.printf(header);
    }
    Set<Integer> keys = new TreeSet<>();
    keys.addAll(fp1.histogram.keySet());
    keys.addAll(fp2.histogram.keySet());

    for (int i : keys) {

      int val1 = fp1.histogram.getOrDefault(i, 0);
      int val2 = fp2.histogram.getOrDefault(i, 0);

      double pct1 = 100.0 * val1 / (double) fp1.qdataTotal;
      double pct2 = 100.0 * val2 / (double) fp2.qdataTotal;

      cumul1 -= pct1;
      cumul2 -= pct2;

      boolean isEquals = (val1 == val2);

      String msg =
          String.format(
              "%6d -> %10d %12.4f %12.4f <-> %10d %12.4f %12.4f    %6b%n",
              i, val1, pct1, cumul1, val2, pct2, cumul2, isEquals);
      if (writer != null) {
        writer.write(msg);
      } else {
        System.out.printf(msg);
      }
    }
  }

  public static double diffImages(
      byte[] imgBytes1, @NonNull Path imgPath2, @NonNull Path outputDiff) throws IOException {
    BufferedImage image2 = ImageIO.read(imgPath2.toFile());
    byte[] rawBytes2 = ((DataBufferByte) image2.getRaster().getDataBuffer()).getData();

    return diffImages(imgBytes1, rawBytes2, image2.getWidth(), image2.getHeight(), outputDiff);
  }

  public static double diffImages(
      byte[] imgBytes1, byte[] imgBytes2, int width, int height, @NonNull Path outputDiff)
      throws IOException {

    double diffCumul = 0;
    byte[] rawBytesDiff = new byte[imgBytes1.length];
    for (int i = 0; i < rawBytesDiff.length; i++) {
      rawBytesDiff[i] = (byte) Math.abs(imgBytes1[i] - imgBytes2[i]);
      diffCumul += rawBytesDiff[i];
    }

    try (OutputStream outputStream = Files.newOutputStream(outputDiff)) {
      BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
      WritableRaster raster = grayImage.getRaster();
      DataBufferByte db = (DataBufferByte) raster.getDataBuffer();
      System.arraycopy(rawBytesDiff, 0, db.getData(), 0, rawBytesDiff.length);

      if (ImageIO.write(grayImage, "png", outputStream)) {
        log.warn("WSQAnalyse failed to write difference image");
      }
    }
    return diffCumul / imgBytes1.length;
  }
}
