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

import static io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder.WSQDecoder.huffman_decode_data_mem;

import io.github.soprasteria.wsq4j.domain.entities.Pair;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.strings.StringRepeat;
import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Map;
import java.util.TreeMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WSQFingerprint {

  public static final String RESUME =
      "WSQ Fingerprint\n"
          + " --------------------------\n"
          + "Size              : %dx%d\n"
          + "Huffman Table 0   : %s\n"
          + "Huffman Table 1   : %s\n"
          + " Quant Table       : %s\n"
          + " Quant Values      : %s\n"
          + " Quant Tree        : %s\n"
          + " Comments          : %s\n"
          + " Histogram         :\n"
          + " %s\n";
  public String huffmanTable0;
  public String huffmanTable1;
  public String quantizationTable;
  public String quantizationValues;
  public String quantTree;
  public String comments;
  public int width;
  public int height;
  public Map<Integer, Integer> histogram;
  public int qdataTotal;

  @Override
  public String toString() {
    return String.format(
        RESUME,
        width,
        height,
        huffmanTable0,
        huffmanTable1,
        quantizationTable,
        quantizationValues,
        quantTree,
        comments,
        drawHistogramConsole(histogram, qdataTotal, 100 /*qdataMax-qdataMin*/));
  }

  public static WSQFingerprint build(DataInput dataInput) throws IOException {
    Pair<WSQHelper.Token, WSQHelper.HeaderFrm> decodePair =
        WSQDecoder.decodeTokenAndHeader(dataInput);
    WSQHelper.Token token = decodePair.getLeft();
    WSQHelper.HeaderFrm header = decodePair.getRight();

    WSQFingerprint fp = new WSQFingerprint();

    fp.width = header.width;
    fp.height = header.height;

    fp.huffmanTable0 = hashTable(token.tableDHT[0]);
    fp.huffmanTable1 = hashTable(token.tableDHT[1]);

    fp.quantizationTable = hashDQT(token.tableDQT);
    fp.quantizationValues = hashQuant(token.quant_vals);
    fp.quantTree = hashQTree(token.qtree);

    int[] qdata = huffman_decode_data_mem(dataInput, token, header.width * header.height);
    fp.histogram = histogram(qdata);
    fp.qdataTotal = qdata.length;

    fp.comments = sha256(String.join("\n", token.comments).getBytes());

    return fp;
  }

  public static Map<Integer, Integer> histogram(int[] qdata) {

    Map<Integer, Integer> hist = new TreeMap<>();
    for (int v : qdata) {
      hist.merge(v, 1, Integer::sum);
    }
    return hist;
  }

  private static String hashTable(WSQHelper.TableDHT table) {
    MessageDigest md = newDigest();

    md.update(table.tabdef);
    for (int v : table.huffbits) update(md, v);
    for (int v : table.huffvalues) update(md, v);
    return hex(md.digest());
  }

  private static String hashDQT(WSQHelper.Table_DQT table) {
    MessageDigest md = newDigest();

    update(md, table.binCenter);
    md.update((byte) table.dqtDef);
    for (float f : table.qBin) update(md, f);
    for (float f : table.zBin) update(md, f);
    return hex(md.digest());
  }

  private static String hashQuant(WSQHelper.Quantization q) {
    MessageDigest md = newDigest();

    update(md, q.q);
    update(md, q.cr);
    update(md, q.r);

    for (float f : q.qbss_t) update(md, f);
    for (float f : q.qbss) update(md, f);
    for (float f : q.qzbs) update(md, f);
    for (float f : q.var) update(md, f);

    return hex(md.digest());
  }

  private static String hashQTree(WSQHelper.QuantTree[] tree) {

    MessageDigest md = newDigest();
    if (tree != null) {
      for (WSQHelper.QuantTree q : tree) {
        update(md, q.x);
        update(md, q.y);
        update(md, q.lenx);
        update(md, q.leny);
      }
    }

    return hex(md.digest());
  }

  private static void update(MessageDigest md, int value) {
    md.update(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value).array());
  }

  private static void update(MessageDigest md, float value) {
    md.update(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(value).array());
  }

  private static MessageDigest newDigest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private static String sha256(byte[] bytes) {
    MessageDigest md = newDigest();
    md.update(bytes);
    return hex(md.digest());
  }

  private static String hex(byte[] hash) {
    Formatter f = new Formatter();
    for (byte b : hash) f.format("%02x", b);
    String s = f.toString();
    f.close();
    return s;
  }

  public static String drawHistogramConsole(Map<Integer, Integer> hist, int total, int maxBars) {

    if (hist == null || hist.isEmpty()) {
      return "Histogram is empty";
    }

    int maxVal = hist.values().stream().max(Integer::compareTo).orElse(1);

    StringBuilder sb = new StringBuilder();
    sb.append("Value | Count | %        Histogram\n");
    sb.append("-----------------------------------------------------\n");

    for (Map.Entry<Integer, Integer> e : hist.entrySet()) {

      int value = e.getKey();
      int count = e.getValue();

      double pct = 100.0 * count / total;

      int barLength = (int) ((count / (double) maxVal) * maxBars);

      String bar = StringRepeat.repeat('█', barLength);

      sb.append(String.format("%5d | %5d | %6.2f%% | %s\n", value, count, pct, bar));
    }
    return sb.toString();
  }
}
