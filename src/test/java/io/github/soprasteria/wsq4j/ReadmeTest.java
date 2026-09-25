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
package io.github.soprasteria.wsq4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.soprasteria.wsq4j.api.Wsq4j;
import io.github.soprasteria.wsq4j.imageio.WSQImageWriteParam;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.junit.jupiter.api.Test;

public class ReadmeTest {
  @Test
  void imageio_read_exemple() throws IOException {
    BufferedImage wsqImage = ImageIO.read(new File("src/test/resources/nbis/ref-finger.wsq"));
    assertNotNull(wsqImage);
  }

  @Test
  void imageio_write_exemple() throws IOException {
    BufferedImage imgInput = ImageIO.read(new File("src/test/resources/nbis/finger.png"));
    ImageIO.write(imgInput, "wsq", new File("target/test-default.wsq"));
    assertThat(new File("target/test-default.wsq")).isNotEmpty();
  }

  @Test
  void imageio_write_with_parameters_exemple() throws IOException {
    BufferedImage imgInput = ImageIO.read(new File("src/test/resources/nbis/finger.png"));

    ImageWriter writer = ImageIO.getImageWritersByFormatName("wsq").next();
    WSQImageWriteParam wsqParams = new WSQImageWriteParam(2.25, 1000);
    try (ImageOutputStream ios =
        ImageIO.createImageOutputStream(new File("target/test-1000ppi-2.25bitrate.wsq"))) {
      writer.setOutput(ios);
      writer.write(null, new javax.imageio.IIOImage(imgInput, null, null), wsqParams);
      writer.dispose();
    }

    assertThat(new File("target/test-1000ppi-2.25bitrate.wsq")).isNotEmpty();
  }

  @Test
  void wsq4japi_decode_exemple() {
    File pngConvertedFile =
        Wsq4j.decoder()
            .decode(new File("src/test/resources/nbis/ref-finger.wsq"))
            .toPng()
            .asFile("target/test-wsq4japi_decode.png");
    assertThat(pngConvertedFile.toPath()).isNotEmptyFile();
  }

  @Test
  void wsq4japi_encode_exemple() {
    Wsq4j.encoder(500, 0.75)
        .encode(new File("src/test/resources/nbis/finger.png"))
        .asFile(new File("target/test-wsq4japi_encode.png"));
    assertThat(Paths.get("target/test-wsq4japi_encode.png")).isNotEmptyFile();
  }
}
