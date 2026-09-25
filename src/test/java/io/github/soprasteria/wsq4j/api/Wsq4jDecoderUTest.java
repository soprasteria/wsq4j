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

import io.github.soprasteria.wsq4j.domain.entities.Bitmap;
import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jFormatException;
import org.junit.jupiter.api.Test;

class Wsq4jDecoderUTest {
  @Test
  void wsq4J_decoder_should_decoder() {
    // Given

    // When
    Bitmap bitmap = Wsq4j.decoder().decode("src/test/resources/nbis/ref-finger.wsq").asBitmap();

    // Then
    assertThat(bitmap).isNotNull();
    assertThat(bitmap.getWidth()).isEqualTo(500);
    assertThat(bitmap.getHeight()).isEqualTo(500);
    assertThat(bitmap.getPixels().length).isEqualTo(500 * 500);
  }

  @Test
  void wsq4J_decoder_with_not_a_wsq_should_failed() {
    // Given
    // When
    RuntimeException exception =
        assertThrows(
            Wsq4jFormatException.class,
            () -> Wsq4j.decoder().decode("src/test/resources/nbis/finger.png"));

    // Then
    assertThat(exception.getMessage()).isEqualTo("Input does not seems to be a valid WSQ file");
  }
}
