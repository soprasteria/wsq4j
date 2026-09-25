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

import static io.github.soprasteria.wsq4j.domain.constants.WSQConstants.*;

import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("DuplicatedCode")
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WSQHelper {

  static class WavletTree {
    int x;
    int y;
    int lenx;
    int leny;
    int invrw;
    int invcl;
  }

  static class TableDTT {

    @SuppressWarnings("unused")
    static final float[]
        HI_FILT_EVEN_8X8_1 =
            {
              0.03226944131446922f,
              -0.05261415011924844f,
              -0.18870142780632693f,
              0.60328894481393847f,
              -0.60328894481393847f,
              0.18870142780632693f,
              0.05261415011924844f,
              -0.03226944131446922f
            },
        LO_FILT_EVEN_8X8_1 =
            {
              0.07565691101399093f,
              -0.12335584105275092f,
              -0.09789296778409587f,
              0.85269867900940344f,
              0.85269867900940344f,
              -0.09789296778409587f,
              -0.12335584105275092f,
              0.07565691101399093f
            },
        // Spec WSQ 3.1 §2
        HI_FILT_NOT_EVEN_8X8_1 =
            {
              0.064538882628938f, // h-4
              -0.040689417609558f, // h-3
              -0.41809227322221f, // h-2
              0.78848561640566f, // h-1
              -0.41809227322221f, // h0
              -0.040689417609558f, // h1
              0.064538882628938f, // h2
            },
        // Spec WSQ 3.1 §2
        LO_FILT_NOT_EVEN_8X8_1 =
            {
              0.037828455506995f, // h4
              -0.02384946501938f, // h3
              -0.11062440441842f, // h2
              0.37740285561265f, // h1
              0.85269867900940f, // h0
              0.37740285561265f, // h-1
              -0.11062440441842f, // h-2
              -0.02384946501938f, // h-3
              0.037828455506995f // h-4
            };

    float[] lofilt = LO_FILT_NOT_EVEN_8X8_1;
    float[] hifilt = HI_FILT_NOT_EVEN_8X8_1;
    int losz;
    int hisz;
    int lodef;
    int hidef;
  }

  static class HuffCode {
    int size;
    int code;
  }

  @SuppressWarnings("unused")
  static class HeaderFrm {
    int black;
    int white;
    int width;
    int height;
    float mShift;
    float rScale;
    int wsqEncoder;
    int software;
  }

  static class HuffmanTable {
    int tableLen;
    int bytesLeft;
    int tableId;
    int[] huffbits;
    int[] huffvalues;
  }

  static class TableDHT {
    byte tabdef;
    int[] huffbits = new int[MAX_HUFFBITS];
    int[] huffvalues = new int[MAX_HUFFCOUNTS_WSQ + 1];
  }

  static class Table_DQT {
    float binCenter;
    final float[] qBin = new float[MAX_SUBBANDS];
    final float[] zBin = new float[MAX_SUBBANDS];
    char dqtDef;
  }

  static class QuantTree {
    int x; /* UL corner of block */
    int y;
    int lenx; /* block size */
    int leny; /* block size */
  }

  static class Quantization {
    float q; /* quantization level */
    float cr; /* compression ratio */
    float r; /* compression bitrate */
    final float[] qbss_t = new float[MAX_SUBBANDS];
    final float[] qbss = new float[MAX_SUBBANDS];
    final float[] qzbs = new float[MAX_SUBBANDS];
    final float[] var = new float[MAX_SUBBANDS];
  }

  static class Ref<T> {
    public T value;

    public Ref() {
      this(null);
    }

    public Ref(T value) {
      this.value = value;
    }
  }

  /** This appears to be the global state of decoder (and now also encoder). */
  static class Token {
    final TableDHT[] tableDHT = new TableDHT[MAX_DHT_TABLES];
    final TableDTT tableDTT = new TableDTT();
    final Table_DQT tableDQT = new Table_DQT();

    WavletTree[] wtree;
    QuantTree[] qtree;

    final Quantization quant_vals = new Quantization();
    final List<String> comments = new ArrayList<>();

    public Token() {
      /* Init DHT Tables to 0. */
      for (int i = 0; i < MAX_DHT_TABLES; i++) {
        tableDHT[i] = new TableDHT();
        tableDHT[i].tabdef = 0;
      }
    }
  }

  static void buildWSQTrees(Token token, int width, int height) {
    /* Build a W-TREE structure for the image. */
    buildWTree(token, W_TREELEN, width, height);
    /* Build a Q-TREE structure for the image. */
    buildQTree(token, Q_TREELEN);
  }

  @SuppressWarnings("SameParameterValue")
  static void buildWTree(Token token, int wtreelen, int width, int height) {
    int lenx, lenx2, leny, leny2; /* starting lengths of sections of
                                              the image being split into subbands */
    token.wtree = new WavletTree[wtreelen];
    for (int i = 0; i < wtreelen; i++) {
      token.wtree[i] = new WavletTree();
      token.wtree[i].invrw = 0;
      token.wtree[i].invcl = 0;
    }

    token.wtree[2].invrw = 1;
    token.wtree[4].invrw = 1;
    token.wtree[7].invrw = 1;
    token.wtree[9].invrw = 1;
    token.wtree[11].invrw = 1;
    token.wtree[13].invrw = 1;
    token.wtree[16].invrw = 1;
    token.wtree[18].invrw = 1;
    token.wtree[3].invcl = 1;
    token.wtree[5].invcl = 1;
    token.wtree[8].invcl = 1;
    token.wtree[9].invcl = 1;
    token.wtree[12].invcl = 1;
    token.wtree[13].invcl = 1;
    token.wtree[17].invcl = 1;
    token.wtree[18].invcl = 1;

    wtree4(token, 0, 1, width, height, 0, 0, 1);

    if ((token.wtree[1].lenx % 2) == 0) {
      lenx = token.wtree[1].lenx / 2;
      lenx2 = lenx;
    } else {
      lenx = (token.wtree[1].lenx + 1) / 2;
      lenx2 = lenx - 1;
    }

    if ((token.wtree[1].leny % 2) == 0) {
      leny = token.wtree[1].leny / 2;
      leny2 = leny;
    } else {
      leny = (token.wtree[1].leny + 1) / 2;
      leny2 = leny - 1;
    }

    wtree4(token, 4, 6, lenx2, leny, lenx, 0, 0);
    wtree4(token, 5, 10, lenx, leny2, 0, leny, 0);
    wtree4(token, 14, 15, lenx, leny, 0, 0, 0);

    token.wtree[19].x = 0;
    token.wtree[19].y = 0;
    if ((token.wtree[15].lenx % 2) == 0) {
      token.wtree[19].lenx = token.wtree[15].lenx / 2;
    } else {
      token.wtree[19].lenx = (token.wtree[15].lenx + 1) / 2;
    }

    if ((token.wtree[15].leny % 2) == 0) {
      token.wtree[19].leny = token.wtree[15].leny / 2;
    } else {
      token.wtree[19].leny = (token.wtree[15].leny + 1) / 2;
    }
  }

  static void wtree4(
      Token token, int start1, int start2, int lenx, int leny, int x, int y, int stop1) {
    int evenx, eveny; /* Check length of subband for even or odd */
    int p1, p2; /* w_tree locations for storing subband sizes and locations */

    p1 = start1;
    p2 = start2;

    evenx = lenx % 2;
    eveny = leny % 2;

    token.wtree[p1].x = x;
    token.wtree[p1].y = y;
    token.wtree[p1].lenx = lenx;
    token.wtree[p1].leny = leny;

    token.wtree[p2].x = x;
    token.wtree[p2 + 2].x = x;
    token.wtree[p2].y = y;
    token.wtree[p2 + 1].y = y;

    if (evenx == 0) {
      token.wtree[p2].lenx = lenx / 2;
      token.wtree[p2 + 1].lenx = token.wtree[p2].lenx;
    } else {
      if (p1 == 4) {
        token.wtree[p2].lenx = (lenx - 1) / 2;
        token.wtree[p2 + 1].lenx = token.wtree[p2].lenx + 1;
      } else {
        token.wtree[p2].lenx = (lenx + 1) / 2;
        token.wtree[p2 + 1].lenx = token.wtree[p2].lenx - 1;
      }
    }
    token.wtree[p2 + 1].x = token.wtree[p2].lenx + x;
    if (stop1 == 0) {
      token.wtree[p2 + 3].lenx = token.wtree[p2 + 1].lenx;
      token.wtree[p2 + 3].x = token.wtree[p2 + 1].x;
    }
    token.wtree[p2 + 2].lenx = token.wtree[p2].lenx;

    if (eveny == 0) {
      token.wtree[p2].leny = leny / 2;
      token.wtree[p2 + 2].leny = token.wtree[p2].leny;
    } else {
      if (p1 == 5) {
        token.wtree[p2].leny = (leny - 1) / 2;
        token.wtree[p2 + 2].leny = token.wtree[p2].leny + 1;
      } else {
        token.wtree[p2].leny = (leny + 1) / 2;
        token.wtree[p2 + 2].leny = token.wtree[p2].leny - 1;
      }
    }
    token.wtree[p2 + 2].y = token.wtree[p2].leny + y;
    if (stop1 == 0) {
      token.wtree[p2 + 3].leny = token.wtree[p2 + 2].leny;
      token.wtree[p2 + 3].y = token.wtree[p2 + 2].y;
    }
    token.wtree[p2 + 1].leny = token.wtree[p2].leny;
  }

  @SuppressWarnings("SameParameterValue")
  static void buildQTree(Token token, int qtreelen) {
    token.qtree = new QuantTree[qtreelen];
    for (int i = 0; i < token.qtree.length; i++) {
      token.qtree[i] = new QuantTree();
    }

    qtree16(
        token,
        3,
        token.wtree[14].lenx,
        token.wtree[14].leny,
        token.wtree[14].x,
        token.wtree[14].y,
        0,
        0);
    qtree16(
        token,
        19,
        token.wtree[4].lenx,
        token.wtree[4].leny,
        token.wtree[4].x,
        token.wtree[4].y,
        0,
        1);
    qtree16(
        token,
        48,
        token.wtree[0].lenx,
        token.wtree[0].leny,
        token.wtree[0].x,
        token.wtree[0].y,
        0,
        0);
    qtree16(
        token,
        35,
        token.wtree[5].lenx,
        token.wtree[5].leny,
        token.wtree[5].x,
        token.wtree[5].y,
        1,
        0);
    qtree4(
        token, 0, token.wtree[19].lenx, token.wtree[19].leny, token.wtree[19].x, token.wtree[19].y);
  }

  static void qtree16(Token token, int start, int lenx, int leny, int x, int y, int rw, int cl) {
    int tempx, temp2x; /* temporary x values */
    int tempy, temp2y; /* temporary y values */
    int evenx, eveny; /* Check length of subband for even or odd */
    int p; /* indicates subband information being stored */

    p = start;
    evenx = lenx % 2;
    eveny = leny % 2;

    if (evenx == 0) {
      tempx = lenx / 2;
      temp2x = tempx;
    } else {
      if (cl != 0) {
        temp2x = (lenx + 1) / 2;
        tempx = temp2x - 1;
      } else {
        tempx = (lenx + 1) / 2;
        temp2x = tempx - 1;
      }
    }

    if (eveny == 0) {
      tempy = leny / 2;
      temp2y = tempy;
    } else {
      if (rw != 0) {
        temp2y = (leny + 1) / 2;
        tempy = temp2y - 1;
      } else {
        tempy = (leny + 1) / 2;
        temp2y = tempy - 1;
      }
    }

    evenx = tempx % 2;
    eveny = tempy % 2;

    token.qtree[p].x = x;
    token.qtree[p + 2].x = x;
    token.qtree[p].y = y;
    token.qtree[p + 1].y = y;
    if (evenx == 0) {
      token.qtree[p].lenx = tempx / 2;
      token.qtree[p + 1].lenx = token.qtree[p].lenx;
      token.qtree[p + 2].lenx = token.qtree[p].lenx;
      token.qtree[p + 3].lenx = token.qtree[p].lenx;
    } else {
      token.qtree[p].lenx = (tempx + 1) / 2;
      token.qtree[p + 1].lenx = token.qtree[p].lenx - 1;
      token.qtree[p + 2].lenx = token.qtree[p].lenx;
      token.qtree[p + 3].lenx = token.qtree[p + 1].lenx;
    }
    token.qtree[p + 1].x = x + token.qtree[p].lenx;
    token.qtree[p + 3].x = token.qtree[p + 1].x;
    if (eveny == 0) {
      token.qtree[p].leny = tempy / 2;
      token.qtree[p + 1].leny = token.qtree[p].leny;
      token.qtree[p + 2].leny = token.qtree[p].leny;
      token.qtree[p + 3].leny = token.qtree[p].leny;
    } else {
      token.qtree[p].leny = (tempy + 1) / 2;
      token.qtree[p + 1].leny = token.qtree[p].leny;
      token.qtree[p + 2].leny = token.qtree[p].leny - 1;
      token.qtree[p + 3].leny = token.qtree[p + 2].leny;
    }
    token.qtree[p + 2].y = y + token.qtree[p].leny;
    token.qtree[p + 3].y = token.qtree[p + 2].y;

    evenx = temp2x % 2;

    token.qtree[p + 4].x = x + tempx;
    token.qtree[p + 6].x = token.qtree[p + 4].x;
    token.qtree[p + 4].y = y;
    token.qtree[p + 5].y = y;
    token.qtree[p + 6].y = token.qtree[p + 2].y;
    token.qtree[p + 7].y = token.qtree[p + 2].y;
    token.qtree[p + 4].leny = token.qtree[p].leny;
    token.qtree[p + 5].leny = token.qtree[p].leny;
    token.qtree[p + 6].leny = token.qtree[p + 2].leny;
    token.qtree[p + 7].leny = token.qtree[p + 2].leny;
    if (evenx == 0) {
      token.qtree[p + 4].lenx = temp2x / 2;
      token.qtree[p + 5].lenx = token.qtree[p + 4].lenx;
      token.qtree[p + 6].lenx = token.qtree[p + 4].lenx;
      token.qtree[p + 7].lenx = token.qtree[p + 4].lenx;
    } else {
      token.qtree[p + 5].lenx = (temp2x + 1) / 2;
      token.qtree[p + 4].lenx = token.qtree[p + 5].lenx - 1;
      token.qtree[p + 6].lenx = token.qtree[p + 4].lenx;
      token.qtree[p + 7].lenx = token.qtree[p + 5].lenx;
    }
    token.qtree[p + 5].x = token.qtree[p + 4].x + token.qtree[p + 4].lenx;
    token.qtree[p + 7].x = token.qtree[p + 5].x;

    eveny = temp2y % 2;

    token.qtree[p + 8].x = x;
    token.qtree[p + 9].x = token.qtree[p + 1].x;
    token.qtree[p + 10].x = x;
    token.qtree[p + 11].x = token.qtree[p + 1].x;
    token.qtree[p + 8].y = y + tempy;
    token.qtree[p + 9].y = token.qtree[p + 8].y;
    token.qtree[p + 8].lenx = token.qtree[p].lenx;
    token.qtree[p + 9].lenx = token.qtree[p + 1].lenx;
    token.qtree[p + 10].lenx = token.qtree[p].lenx;
    token.qtree[p + 11].lenx = token.qtree[p + 1].lenx;
    if (eveny == 0) {
      token.qtree[p + 8].leny = temp2y / 2;
      token.qtree[p + 9].leny = token.qtree[p + 8].leny;
      token.qtree[p + 10].leny = token.qtree[p + 8].leny;
      token.qtree[p + 11].leny = token.qtree[p + 8].leny;
    } else {
      token.qtree[p + 10].leny = (temp2y + 1) / 2;
      token.qtree[p + 11].leny = token.qtree[p + 10].leny;
      token.qtree[p + 8].leny = token.qtree[p + 10].leny - 1;
      token.qtree[p + 9].leny = token.qtree[p + 8].leny;
    }
    token.qtree[p + 10].y = token.qtree[p + 8].y + token.qtree[p + 8].leny;
    token.qtree[p + 11].y = token.qtree[p + 10].y;

    token.qtree[p + 12].x = token.qtree[p + 4].x;
    token.qtree[p + 13].x = token.qtree[p + 5].x;
    token.qtree[p + 14].x = token.qtree[p + 4].x;
    token.qtree[p + 15].x = token.qtree[p + 5].x;
    token.qtree[p + 12].y = token.qtree[p + 8].y;
    token.qtree[p + 13].y = token.qtree[p + 8].y;
    token.qtree[p + 14].y = token.qtree[p + 10].y;
    token.qtree[p + 15].y = token.qtree[p + 10].y;
    token.qtree[p + 12].lenx = token.qtree[p + 4].lenx;
    token.qtree[p + 13].lenx = token.qtree[p + 5].lenx;
    token.qtree[p + 14].lenx = token.qtree[p + 4].lenx;
    token.qtree[p + 15].lenx = token.qtree[p + 5].lenx;
    token.qtree[p + 12].leny = token.qtree[p + 8].leny;
    token.qtree[p + 13].leny = token.qtree[p + 8].leny;
    token.qtree[p + 14].leny = token.qtree[p + 10].leny;
    token.qtree[p + 15].leny = token.qtree[p + 10].leny;
  }

  @SuppressWarnings("SameParameterValue")
  static void qtree4(Token token, int start, int lenx, int leny, int x, int y) {
    int evenx, eveny; /* Check length of subband for even or odd */
    int p; /* indicates subband information being stored */

    p = start;
    evenx = lenx % 2;
    eveny = leny % 2;

    token.qtree[p].x = x;
    token.qtree[p + 2].x = x;
    token.qtree[p].y = y;
    token.qtree[p + 1].y = y;
    if (evenx == 0) {
      token.qtree[p].lenx = lenx / 2;
      token.qtree[p + 1].lenx = token.qtree[p].lenx;
      token.qtree[p + 2].lenx = token.qtree[p].lenx;
      token.qtree[p + 3].lenx = token.qtree[p].lenx;
    } else {
      token.qtree[p].lenx = (lenx + 1) / 2;
      token.qtree[p + 1].lenx = token.qtree[p].lenx - 1;
      token.qtree[p + 2].lenx = token.qtree[p].lenx;
      token.qtree[p + 3].lenx = token.qtree[p + 1].lenx;
    }
    token.qtree[p + 1].x = x + token.qtree[p].lenx;
    token.qtree[p + 3].x = token.qtree[p + 1].x;
    if (eveny == 0) {
      token.qtree[p].leny = leny / 2;
      token.qtree[p + 1].leny = token.qtree[p].leny;
      token.qtree[p + 2].leny = token.qtree[p].leny;
      token.qtree[p + 3].leny = token.qtree[p].leny;
    } else {
      token.qtree[p].leny = (leny + 1) / 2;
      token.qtree[p + 1].leny = token.qtree[p].leny;
      token.qtree[p + 2].leny = token.qtree[p].leny - 1;
      token.qtree[p + 3].leny = token.qtree[p + 2].leny;
    }
    token.qtree[p + 2].y = y + token.qtree[p].leny;
    token.qtree[p + 3].y = token.qtree[p + 2].y;
  }
}
