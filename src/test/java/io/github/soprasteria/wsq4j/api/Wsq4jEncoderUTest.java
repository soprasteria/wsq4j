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
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.soprasteria.wsq4j.domain.entities.Pair;
import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jInvalidArgumentException;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.WSQCheckMagicByte;
import io.github.soprasteria.wsq4j.test_utils.fixtures.FixturesUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class Wsq4jEncoderUTest {

  @Test
  void wsq4j_encoder_toStream_should_encode() {
    // Given
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // When
    Wsq4j.encoder(500, 0.75).encode("src/test/resources/nbis/finger.png").asStream(outputStream);

    // Then
    byte[] wsqResult = outputStream.toByteArray();
    assertThat(wsqResult).isNotNull();
    assertThat(WSQCheckMagicByte.execute(wsqResult)).isTrue();
  }

  @Test
  void wsq4j_encoder_toBytes_should_encode() {
    // Given

    // When
    byte[] wsqResult =
        Wsq4j.encoder(500, 0.75).encode("src/test/resources/nbis/finger.png").asByteArray();

    // Then
    assertThat(wsqResult).isNotNull();
    assertThat(WSQCheckMagicByte.execute(wsqResult)).isTrue();
  }

  @Test
  void wsq4j_encoder_toFile_should_encode() throws IOException {
    // Given
    Path inputPngPath = Paths.get("src/test/resources/nbis/finger.png");
    Path outputWsqPath =
        FixturesUtils.getTestPath("wsq4j_encoder_toFile", inputPngPath, Pair.of(".png", ".wsq"));
    Files.deleteIfExists(outputWsqPath);

    // When
    Wsq4j.encoder(500, 0.75, null).encode(inputPngPath.toFile()).asFile(outputWsqPath.toFile());

    // Then
    assertThat(outputWsqPath).isNotEmptyFile();
  }

  @Test
  void wsq4j_encoder_with_invalid_should_failed() throws IOException {
    // Given
    Path inputPngPath = Paths.get("src/test/resources/nbis/finger.png");
    Path outputWsqPath =
        FixturesUtils.getTestPath(
            "wsq4j_encoder_with_invalid", inputPngPath, Pair.of(".png", ".wsq"));

    // When
    Exception exceptionPPI =
        assertThrows(
            Wsq4jInvalidArgumentException.class,
            () ->
                Wsq4j.encoder(-10, 0.75)
                    .encode(inputPngPath.toFile())
                    .asFile(outputWsqPath.toFile()));
    Exception exceptionBitRate =
        assertThrows(
            Wsq4jInvalidArgumentException.class,
            () ->
                Wsq4j.encoder(500, -10)
                    .encode(inputPngPath.toFile())
                    .asFile(outputWsqPath.toFile()));
    // Then
    assertThat(exceptionPPI.getMessage()).isEqualTo("PPI is too low -10");
    assertThat(exceptionBitRate.getMessage()).isEqualTo("BITRATE not valid: -10.0");
  }
}
