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

import io.github.soprasteria.wsq4j.domain.entities.Pair;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.ReadAllBytesHelper;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder.WSQAnalyse;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import org.junit.jupiter.api.Test;

class WSQImageReaderITest {

  @Test
  void imageio_read_on_ref_finger_should_return_a_valid_image_then_diff_with_png()
      throws IOException {
    // Given
    Path rawInputPath = Paths.get("src/test/resources/nbis/finger.raw");
    Path wsqFileToRead = Paths.get("src/test/resources/nbis/ref-finger.wsq");
    Path pngFileToWrite =
        getTestPath("imageio_read_on_ref_finger", wsqFileToRead, Pair.of(".wsq", ".png"));
    Path diffPngOutputPath =
        getTestPath("imageio_read_on_ref_finger", wsqFileToRead, Pair.of(".wsq", "-diff.png"));
    double diffAvg;

    // When
    BufferedImage readImage = ImageIO.read(wsqFileToRead.toFile());

    // Then
    assertThat(readImage).isNotNull();
    assertThat(readImage.getWidth()).isEqualTo(500);
    assertThat(readImage.getHeight()).isEqualTo(500);

    try (OutputStream outputStream = Files.newOutputStream(pngFileToWrite)) {
      boolean success = ImageIO.write(readImage, "png", outputStream);
      assertTrue(success);
    }
    assertThat(pngFileToWrite).isNotEmptyFile();

    // Then
    try (DataInputStream rawIS = new DataInputStream(Files.newInputStream(rawInputPath))) {
      diffAvg =
          WSQAnalyse.diffImages(
              ReadAllBytesHelper.execute(rawIS), wsqFileToRead, diffPngOutputPath);
    }
    assertThat(diffAvg).as("Images are too much different").isLessThan(10);
  }

  @Test
  void imageio_reader_on_refFinger_should_return_the_full_metadata() throws IOException {
    // Given
    Path wsqFileToRead = Paths.get("src/test/resources/nbis/ref-finger.wsq");

    // When
    ImageReader reader = ImageIO.getImageReadersByFormatName("wsq").next();
    assertThat(reader).isNotNull();
    try (ImageInputStream iis = ImageIO.createImageInputStream(wsqFileToRead.toFile())) {
      reader.setInput(iis, true, true);
      assertThat(reader.getNumImages(true)).isEqualTo(1);

      IIOMetadata metadata = reader.getImageMetadata(0);

      // Then
      assertThat(metadata).isInstanceOf(WSQMetadata.class);

      WSQMetadata wsqMetadata = (WSQMetadata) metadata;
      assertThat(wsqMetadata.getPPI()).isEqualTo(500);
      assertThat(wsqMetadata.getBitrate()).isEqualTo(2.25);
      assertThat(wsqMetadata.getNistcom())
          .isEqualTo(
              "NIST_COM 8\n"
                  + "PIX_WIDTH 500\n"
                  + "PIX_HEIGHT 500\n"
                  + "PIX_DEPTH 8\n"
                  + "PPI 500\n"
                  + "LOSSY 1\n"
                  + "COLORSPACE GRAY\n"
                  + "COMPRESSION WSQ\n"
                  + "WSQ_BITRATE 2.250000\n");

      reader.dispose();
    }
  }
}
