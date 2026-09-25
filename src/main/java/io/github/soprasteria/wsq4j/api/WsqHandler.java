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

import io.github.soprasteria.wsq4j.domain.entities.Bitmap;
import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jEncodeException;
import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jException;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder.WSQEncoder;
import java.io.*;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class WsqHandler {
  private final Bitmap bitmap;
  private final double bitRate;
  private final String comments;

  private byte[] convertToWSQ() {

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      WSQEncoder.encode(outputStream, bitmap, bitRate, comments);
      return outputStream.toByteArray();

    } catch (Wsq4jException w4e) {
      throw w4e;
    } catch (Exception e) {
      throw new Wsq4jEncodeException("Exception while encode", e);
    }
  }

  public void asStream(OutputStream outputStream) {
    try {
      outputStream.write(convertToWSQ());
    } catch (Wsq4jException w4e) {
      throw w4e;
    } catch (Exception e) {
      throw new Wsq4jEncodeException(e);
    }
  }

  public byte[] asByteArray() {
    return convertToWSQ();
  }

  @SuppressWarnings("UnusedReturnValue")
  public File asFile(File file) {
    try (FileOutputStream outputStream = new FileOutputStream(file)) {
      asStream(outputStream);
    } catch (Wsq4jException w4j) {
      throw w4j;
    } catch (Exception e) {
      throw new Wsq4jEncodeException(e);
    }
    return file;
  }
}
