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
package io.github.soprasteria.wsq4j.test_utils.helpers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.soprasteria.wsq4j.domain.entities.Bitmap;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder.WSQDecoder;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DecodeToBufferedHelper {
  @SuppressWarnings("UnusedReturnValue")
  public static Path decodeWSQ(Path wsqPath, Path outputPath) throws IOException {

    Files.deleteIfExists(outputPath);

    try (InputStream inputStream = Files.newInputStream(wsqPath);
        OutputStream outputStream = Files.newOutputStream(outputPath)) {

      Bitmap bitmap = WSQDecoder.decode(inputStream);
      int width = bitmap.getWidth();
      int height = bitmap.getHeight();

      final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

      WritableRaster raster = image.getRaster();
      raster.setDataElements(0, 0, width, height, bitmap.getPixels());

      boolean isOk = ImageIO.write(image, "PGM", outputStream);
      assertTrue(isOk);

      log.debug("decodeWSQ {} to {}", wsqPath.getFileName(), outputPath);
      return outputPath;
    }
  }
}
