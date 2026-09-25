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
package io.github.soprasteria.wsq4j.imageio;

import static io.github.soprasteria.wsq4j.test_utils.fixtures.FixturesUtils.getTestPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import io.github.soprasteria.wsq4j.domain.entities.BitmapWithMetadata;
import io.github.soprasteria.wsq4j.domain.entities.Pair;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder.WSQDecoder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.junit.jupiter.api.Test;

class WSQImageWriterITest {
  @Test
  void imageio_write_without_params_should_return_the_right_value() throws IOException {
    // Given
    Path pngFileToRead = Paths.get("src/test/resources/nbis/finger.png");
    Path wsqFileToWrite =
        getTestPath("imageio_write_without_params", pngFileToRead, Pair.of(".png", ".wsq"));

    try (OutputStream outputStream = Files.newOutputStream(wsqFileToWrite)) {
      BufferedImage imgInput = ImageIO.read(pngFileToRead.toFile());

      // When
      boolean success = ImageIO.write(imgInput, "wsq", outputStream);

      // Then
      assertTrue(success);
    }

    assertThat(wsqFileToWrite).isNotEmptyFile();
  }

  @Test
  void imageio_writer_with_params_should_return_the_right_value() throws IOException {
    // Given
    Path pngFileToRead = Paths.get("src/test/resources/nbis/finger.png");
    Path wsqFileToWrite =
        getTestPath("imageio_write_with_params", pngFileToRead, Pair.of(".png", ".wsq"));

    BufferedImage imgInput = ImageIO.read(pngFileToRead.toFile());

    try (ImageOutputStream ios = ImageIO.createImageOutputStream(wsqFileToWrite.toFile())) {
      assertThat(ios).isNotNull();

      // When
      ImageWriter writer = ImageIO.getImageWritersByFormatName("wsq").next();
      WSQImageWriteParam wsqParams = new WSQImageWriteParam(0.75, 500);
      writer.setOutput(ios);
      writer.write(null, new javax.imageio.IIOImage(imgInput, null, null), wsqParams);
    }

    // Then
    assertThat(wsqFileToWrite).isNotEmptyFile();

    BitmapWithMetadata bitmapWithMetadata = WSQDecoder.decode(Files.newInputStream(wsqFileToWrite));
    assertThat(bitmapWithMetadata.getWidth()).isEqualTo(imgInput.getWidth());
    assertThat(bitmapWithMetadata.getHeight()).isEqualTo(imgInput.getHeight());
    assertThat(bitmapWithMetadata.getPpi()).isEqualTo(500);
    assertThat(bitmapWithMetadata.getLossyflag()).isEqualTo(1);
    assertThat(bitmapWithMetadata.getDepth()).isEqualTo(8);
    assertThat(bitmapWithMetadata.getComments()).hasSize(1);
    assertThat(bitmapWithMetadata.getComments().get(0)).startsWith("wsq4j");
    assertThat(bitmapWithMetadata.getBitRate()).isEqualTo(0.75);
  }

  @Test
  void imageio_write_without_color_png_should_return_the_right_value() throws IOException {
    // Given
    Path pngFileToRead = Paths.get("src/test/resources/nbis/finger-color.png");
    Path wsqFileToWrite =
        getTestPath("imageio_write_without_color_png", pngFileToRead, Pair.of(".png", ".wsq"));

    try (OutputStream outputStream = Files.newOutputStream(wsqFileToWrite)) {
      BufferedImage imgInput = ImageIO.read(pngFileToRead.toFile());

      // When
      boolean success = ImageIO.write(imgInput, "wsq", outputStream);

      // Then
      assertTrue(success);
    }

    assertThat(wsqFileToWrite).isNotEmptyFile();
  }

  @Test
  void imageio_write_convert_png_gray_to_color() throws IOException {
    // Given
    Path pngFileToRead = Paths.get("src/test/resources/nbis/finger.png");
    Path pngFileToWrite =
        getTestPath(
            "imageio_write_convert_png_gray_to_color",
            pngFileToRead,
            Pair.of(".png", "-color.png"));

    BufferedImage imgInput = ImageIO.read(pngFileToRead.toFile());

    try (OutputStream outputStream = Files.newOutputStream(pngFileToWrite)) {

      // When
      BufferedImage colorImage =
          new BufferedImage(imgInput.getWidth(), imgInput.getHeight(), BufferedImage.TYPE_INT_RGB);

      Graphics2D g = colorImage.createGraphics();
      g.drawImage(imgInput, 0, 0, null);
      g.dispose();

      boolean success = ImageIO.write(colorImage, "png", outputStream);

      // Then
      assertTrue(success);
    }

    assertThat(pngFileToWrite).isNotEmptyFile();
  }
}
