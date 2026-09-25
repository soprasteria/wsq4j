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

import static io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder.WSQEncoder.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.soprasteria.wsq4j.domain.entities.Bitmap;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.Wsq4jConfig;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder.WSQEncoder;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EncodeFromBufferedHelper {
  private static final int PPI = 500;

  public static Path encodeWSQ(Path inputPath, int compression, Path outputPath)
      throws IOException {

    Files.deleteIfExists(outputPath);

    try (InputStream inputStream = Files.newInputStream(inputPath);
        OutputStream outputStream = Files.newOutputStream(outputPath)) {
      BufferedImage bufferedImage = ImageIO.read(inputStream);
      assertThat(bufferedImage).isNotNull();

      DataBufferByte data = (DataBufferByte) bufferedImage.getRaster().getDataBuffer();
      Bitmap bitmap =
          new Bitmap(
              data.getData(),
              bufferedImage.getWidth(),
              bufferedImage.getHeight(),
              PPI,
              DEFAULT_DEEPTH,
              LOSSYFLAG_NO_LOSS);

      WSQEncoder.encode(
          outputStream, bitmap, ((double) compression) / 100, Wsq4jConfig.WSQ_COMMENT);

      log.debug("encodeWSQ {} to {}", inputPath.getFileName(), outputPath);
    }

    return outputPath;
  }
}
