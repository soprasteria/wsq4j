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
package io.github.soprasteria.wsq4j.api;

import static io.github.soprasteria.wsq4j.domain.constants.WSQConstants.*;

import io.github.soprasteria.wsq4j.domain.use_cases.helpers.Wsq4jConfig;
import lombok.NoArgsConstructor;

@SuppressWarnings("unused")
@NoArgsConstructor
public class Wsq4j {

  public static WSQDecoderHandler decoder() {
    return new WSQDecoderHandler();
  }

  public static WSQEncoderHandler encoder(int ppi, double bitRate, String comment) {
    return new WSQEncoderHandler(ppi, bitRate, comment);
  }

  public static WSQEncoderHandler encoder(int ppi, double bitRate) {
    return new WSQEncoderHandler(ppi, bitRate, Wsq4jConfig.WSQ_COMMENT);
  }

  public static WSQEncoderHandler encoder(double bitRate) {
    return new WSQEncoderHandler(DEFAULT_PPI, bitRate, Wsq4jConfig.WSQ_COMMENT);
  }

  public static WSQEncoderHandler encoder(int ppi) {
    return new WSQEncoderHandler(ppi, DEFAULT_BITRATE, Wsq4jConfig.WSQ_COMMENT);
  }

  public static WSQEncoderHandler encoder() {
    return new WSQEncoderHandler(DEFAULT_PPI, DEFAULT_BITRATE, Wsq4jConfig.WSQ_COMMENT);
  }
}
