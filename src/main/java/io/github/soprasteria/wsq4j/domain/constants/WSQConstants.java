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
package io.github.soprasteria.wsq4j.domain.constants;

@SuppressWarnings("unused")
public interface WSQConstants {
  int DEFAULT_DEEPTH = 8;
  int UNKNOW_PPI = -1; // unknow PPI
  int DEFAULT_PPI = 500; // unknow PPI
  double DEFAULT_BITRATE = 0.75;
  String NO_COMMENT = "";
  int LOSSYFLAG_NO_LOSS = 0;
  int LOSSYFLAG_WITH_LOSS = 1;
  String GRAY_COLORSPACE = "GRAY";
  String COMPRESSION_CODE = "WSQ";

  /*used to "mask out" n number of bits from data stream*/
  int[] BITMASK = {0x00, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, 0xff};

  int MAX_DHT_TABLES = 8;
  int MAX_HUFFBITS = 16;
  int MAX_HUFFCOUNTS_WSQ = 256;

  int MAX_HUFFCOEFF = 74; /* -73 .. +74 */
  int MAX_HUFFZRUN = 100;

  int MAX_HIFILT = 7;
  int MAX_LOFILT = 9;

  int W_TREELEN = 20;
  int Q_TREELEN = 64;

  /* WSQ Marker Definitions */
  int SOI_WSQ = 0xffa0;
  int EOI_WSQ = 0xffa1;
  int SOF_WSQ = 0xffa2;
  int SOB_WSQ = 0xffa3;
  int DTT_WSQ = 0xffa4;
  int DQT_WSQ = 0xffa5;
  int DHT_WSQ = 0xffa6;
  int DRT_WSQ = 0xffa7;
  int COM_WSQ = 0xffa8;

  int STRT_SUBBAND_2 = 19;
  int STRT_SUBBAND_3 = 52;
  int MAX_SUBBANDS = 64;
  int NUM_SUBBANDS = 60;
  int STRT_SUBBAND_DEL = NUM_SUBBANDS;
  int STRT_SIZE_REGION_2 = 4;
  int STRT_SIZE_REGION_3 = 51;

  int COEFF_CODE = 0;
  int RUN_CODE = 1;

  float VARIANCE_THRESH = 1.01f;

  /* Case for getting ANY marker. */
  int ANY_WSQ = 0xffff;
  int TBLS_N_SOF = 2;
  int TBLS_N_SOB = TBLS_N_SOF + 2;

  // Specification IAFIS-IC-0110 (V3.1)
  // Ev (Encoder Version Number) must be 2, not 0
  int EV_VALUE = 2;
  // Sf (Software Implementation Number) must > 1, and not 0
}
