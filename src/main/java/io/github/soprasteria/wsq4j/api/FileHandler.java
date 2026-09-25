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

import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jException;
import java.io.*;

@SuppressWarnings("unused")
public class FileHandler {
  private final byte[] data;

  public FileHandler(byte[] data) {
    this.data = data;
  }

  public File asFile(File file) {
    try (FileOutputStream bos = new FileOutputStream(file)) {
      bos.write(data);
    } catch (Exception e) {
      throw new Wsq4jException("unexpected error", e);
    }
    return file;
  }

  public File asFile(String fileName) {
    File file = new File(fileName);
    return asFile(file);
  }

  public InputStream asInputStream() {
    return new ByteArrayInputStream(data);
  }

  public byte[] asByteArray() {
    return data;
  }
}
