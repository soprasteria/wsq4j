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

import static io.github.soprasteria.wsq4j.domain.constants.NISTConstants.NCM_WSQ_RATE;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public class BitmapWithMetadata extends Bitmap {
  private static final long serialVersionUID = -4243273616650162026L;

  private final Map<String, String> metadata = new LinkedHashMap<>();
  private final List<String> comments = new ArrayList<>();

  public BitmapWithMetadata(
      byte[] pixels, int width, int height, int ppi, int depth, int lossyflag) {
    this(pixels, width, height, ppi, depth, lossyflag, null);
  }

  public BitmapWithMetadata(
      byte[] pixels,
      int width,
      int height,
      int ppi,
      int depth,
      int lossyflag,
      Map<String, String> metadata,
      String... comments) {
    super(pixels, width, height, ppi, depth, lossyflag);
    if (metadata != null) {
      this.metadata.putAll(metadata);
    }
    if (comments != null) {
      for (String s : comments) {
        if (s != null) {
          this.comments.add(s);
        }
      }
    }
  }

  public double getBitRate() {
    String bitRateStr = this.metadata.get(NCM_WSQ_RATE);
    if (bitRateStr == null) {
      return -1.0;
    } else {
      try {
        return Double.parseDouble(bitRateStr);
      } catch (NumberFormatException e) {
        return -1.0;
      }
    }
  }
}
