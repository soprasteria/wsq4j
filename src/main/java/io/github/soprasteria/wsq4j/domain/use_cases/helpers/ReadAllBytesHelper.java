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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ReadAllBytesHelper {
  public static byte[] execute(DataInputStream dis) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    byte[] data = new byte[4096];
    int n;
    while ((n = dis.read(data)) != -1) {
      buffer.write(data, 0, n);
    }

    return buffer.toByteArray();
  }

  public static byte[] execute(InputStream is) throws IOException {
    try (DataInputStream dis = new DataInputStream(is)) {
      return execute(dis);
    }
  }
}
