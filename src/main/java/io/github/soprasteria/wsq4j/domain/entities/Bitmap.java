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
package io.github.soprasteria.wsq4j.domain.entities;

import java.io.Serializable;
import lombok.Getter;

@Getter
public class Bitmap implements Serializable {

  private static final long serialVersionUID = -8632563339133022850L;

  private final int width;
  private final int height;
  private final int ppi;
  private final int depth;
  private final int lossyflag;

  private final byte[] pixels;
  private final int length;

  public Bitmap(byte[] pixels, int width, int height, int ppi, int depth, int lossyflag) {
    this.pixels = pixels;
    this.length = pixels != null ? pixels.length : 0;

    this.width = width;
    this.height = height;
    this.ppi = ppi;
    this.depth = depth;
    this.lossyflag = lossyflag;
  }

  @SuppressWarnings("StringBufferReplaceableByString")
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("Bitmap [");
    result.append(width);
    result.append(" x ");
    result.append(height);
    result.append(" x ");
    result.append(depth);
    result.append(", ");
    result.append("ppi = ");
    result.append(ppi);
    result.append(", ");
    result.append("lossy = ");
    result.append(lossyflag);
    result.append("]");
    return result.toString();
  }
}
