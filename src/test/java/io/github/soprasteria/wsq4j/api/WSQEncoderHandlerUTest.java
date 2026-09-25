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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jEncodeException;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.ReadAllBytesHelper;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.WSQCheckMagicByte;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class WSQEncoderHandlerUTest {

  @Test
  void encode_withFile_should_generate_file() {
    // Given
    File imgPngFile = new File("src/test/resources/nbis/finger.png");
    assertThat(imgPngFile).isNotEmpty();

    // When
    byte[] wsqBytes = Wsq4j.encoder().encode(imgPngFile).asByteArray();

    // Then
    assertThat(wsqBytes).isNotEmpty();
    assertThat(WSQCheckMagicByte.execute(wsqBytes)).isTrue();
  }

  @Test
  void encode_withMissingFile_should_throws_exception() {
    // Given
    File imgPngFile = new File("src/test/resources/nbis/not_exists_file.png");
    assertThat(imgPngFile).doesNotExist();

    // When
    Exception exception =
        assertThrows(
            Wsq4jEncodeException.class, () -> Wsq4j.encoder().encode(imgPngFile).asByteArray());

    // Then
    assertThat(exception).hasMessage("javax.imageio.IIOException: Can't read input file!");
  }

  @Test
  void encode_withFilename_should_generate_file() {
    // Given
    String pngFileName = "src/test/resources/nbis/finger.png";

    // When
    byte[] wsqBytes = Wsq4j.encoder().encode(pngFileName).asByteArray();

    // Then
    assertThat(wsqBytes).isNotEmpty();
    assertThat(WSQCheckMagicByte.execute(wsqBytes)).isTrue();
  }

  @Test
  void encode_withInputStream_should_generate_file() throws IOException {
    // Given
    InputStream is = Files.newInputStream(Paths.get("src/test/resources/nbis/finger.png"));

    // When
    byte[] wsqBytes = Wsq4j.encoder().encode(is).asByteArray();

    // Then
    assertThat(wsqBytes).isNotEmpty();
    assertThat(WSQCheckMagicByte.execute(wsqBytes)).isTrue();
  }

  @Test
  void encode_withNullInputStream_should_throws_exception() {
    // Given
    InputStream is = null;

    // When
    Exception exception =
        assertThrows(Wsq4jEncodeException.class, () -> Wsq4j.encoder().encode(is).asByteArray());

    // Then
    assertThat(exception).hasMessage("java.lang.IllegalArgumentException: input == null!");
  }

  @Test
  void encode_withBytes_should_generate_file() throws IOException {
    // Given
    InputStream is = Files.newInputStream(Paths.get("src/test/resources/nbis/finger.png"));
    byte[] imgPngBytes = ReadAllBytesHelper.execute(is);

    // When
    byte[] wsqBytes = Wsq4j.encoder().encode(imgPngBytes, "PNG").asByteArray();

    // Then
    assertThat(wsqBytes).isNotEmpty();
    assertThat(WSQCheckMagicByte.execute(wsqBytes)).isTrue();
  }

  @Test
  void encode_withBytesAndBadFormat_should_throws_exception() throws IOException {
    // Given
    InputStream is = Files.newInputStream(Paths.get("src/test/resources/nbis/finger.png"));
    byte[] imgPngBytes = ReadAllBytesHelper.execute(is);

    // When
    Exception exception =
        assertThrows(
            Wsq4jEncodeException.class,
            () -> Wsq4j.encoder().encode(imgPngBytes, "BAD_FORMAT").asByteArray());

    // Then
    assertThat(exception).hasMessage("Unknow format BAD_FORMAT");
  }
}
