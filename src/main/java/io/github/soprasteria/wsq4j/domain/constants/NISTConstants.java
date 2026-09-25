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
public interface NISTConstants {
  /* From nistcom.h */
  String NCM_EXT = "ncm";
  String NCM_HEADER = "NIST_COM"; /* mandatory */
  String NCM_PIX_WIDTH = "PIX_WIDTH"; /* mandatory */
  String NCM_PIX_HEIGHT = "PIX_HEIGHT"; /* mandatory */
  String NCM_PIX_DEPTH = "PIX_DEPTH"; /* 1,8,24 (mandatory)*/
  String NCM_PPI = "PPI"; /* -1 if unknown (mandatory)*/
  String NCM_COLORSPACE = "COLORSPACE"; /* RGB,YCbCr,GRAY */
  String NCM_N_CMPNTS = "NUM_COMPONENTS"; /* [1..4] (mandatory w/hv_factors)*/
  String NCM_HV_FCTRS = "HV_FACTORS"; /* H0,V0:H1,V1:...*/
  String NCM_INTRLV = "INTERLEAVE"; /* 0,1 (mandatory w/depth=24) */
  String NCM_COMPRESSION = "COMPRESSION"; /* NONE,JPEGB,JPEGL,WSQ */
  String NCM_JPEGB_QUAL = "JPEGB_QUALITY"; /* [20..95] */
  String NCM_JPEGL_PREDICT = "JPEGL_PREDICT"; /* [1..7] */
  String NCM_WSQ_RATE = "WSQ_BITRATE"; /* ex. .75,2.25 (-1.0 if unknown)*/
  String NCM_LOSSY = "LOSSY"; /* 0,1 */

  String NCM_HISTORY = "HISTORY"; /* ex. SD historical data */
  String NCM_FING_CLASS = "FING_CLASS"; /* ex. A,L,R,S,T,W */
  String NCM_SEX = "SEX"; /* m,f */
  String NCM_SCAN_TYPE = "SCAN_TYPE"; /* l,i */
  String NCM_FACE_POS = "FACE_POS"; /* f,p */
  String NCM_AGE = "AGE";
  String NCM_SD_ID = "SD_ID"; /* 4,9,10,14,18 */
}
