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

import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jDecodeException;
import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jException;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.WSQCheckMagicByte;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder.WSQDecoder;
import java.io.*;
import java.nio.file.Files;

public class WSQDecoderHandler {

  public BitmapHandler decode(String fileName) {
    return decode(new File(fileName));
  }

  public BitmapHandler decode(File file) {
    try {
      return decode(Files.newInputStream(file.toPath()));
    } catch (Wsq4jException w4j) {
      throw w4j;
    } catch (Exception e) {
      throw new Wsq4jException("unexpected error.", e);
    }
  }

  public BitmapHandler decode(InputStream inputStream) {
    try (BufferedInputStream input = new BufferedInputStream(inputStream);
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {

      byte[] data = new byte[16384];
      int nRead;
      while ((nRead = input.read(data, 0, data.length)) != -1) {
        output.write(data, 0, nRead);
      }
      output.flush();

      byte[] imageBytes = output.toByteArray();
      WSQCheckMagicByte.verifyOrThrow(imageBytes);
      return decode(imageBytes);
    } catch (Wsq4jException w4j) {
      throw w4j;
    } catch (Exception e) {
      throw new Wsq4jDecodeException("unexpected error.", e);
    }
  }

  public BitmapHandler decode(final byte[] data) {
    return new BitmapHandler(WSQDecoder.decode(data));
  }
}
