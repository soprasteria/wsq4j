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
package io.github.soprasteria.wsq4j.domain.use_cases.helpers;

import static io.github.soprasteria.wsq4j.domain.constants.WSQConstants.SOI_WSQ;

import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jFormatException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WSQCheckMagicByte {
  public static boolean execute(int magicByte) {
    return SOI_WSQ == magicByte;
  }

  public static boolean execute(byte[] imageBytes) {
    return execute(imageBytes[0], imageBytes[1]);
  }

  public static boolean execute(byte b0, byte b1) {
    int magicByte = ((b0 & 0xFF) << 8) | (b1 & 0xFF);
    return execute(magicByte);
  }

  public static void verifyOrThrow(byte[] imageBytes) {
    if (execute(imageBytes)) {
      log.debug("Image input is a valid WSQ file");
    } else {
      throw new Wsq4jFormatException("Input does not seems to be a valid WSQ file");
    }
  }
}
