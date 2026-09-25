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

import java.awt.*;
import java.awt.image.BufferedImage;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConvertImageToGrayscaleHelper {

  public static BufferedImage execute(@NonNull BufferedImage inputImage) {
    if (inputImage.getType() == BufferedImage.TYPE_BYTE_GRAY) {
      log.debug("Image is already grayscale, nothing to do");
      return inputImage;
    } else {
      final int width = inputImage.getWidth();
      final int height = inputImage.getHeight();
      log.debug("Image is not grayscale. Converting {}x{}", width, height);

      final BufferedImage grayImage =
          new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

      inputImage.copyData(grayImage.getRaster());

      Graphics g = grayImage.getGraphics();
      g.drawImage(inputImage, 0, 0, null);
      g.dispose();

      return grayImage;
    }
  }
}
