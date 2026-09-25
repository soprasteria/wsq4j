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

import static io.github.soprasteria.wsq4j.test_utils.fixtures.FixturesUtils.getTestPath;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.soprasteria.wsq4j.domain.entities.Pair;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder.WSQAnalyse;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder.WSQFingerprint;
import io.github.soprasteria.wsq4j.test_utils.helpers.DecodeToBufferedHelper;
import io.github.soprasteria.wsq4j.test_utils.helpers.EncodeFromBufferedHelper;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Slf4j
public class WSQEncodingDecodingAnalyseITest {

  @ValueSource(strings = {"cmp00009.wsq", "cmp00009-cognaxon.wsq"})
  @ParameterizedTest
  void decode_encode_analyse(String inputWsqFile) throws IOException {
    // Given
    Path inputPath =
        Paths.get("src/test/resources/test_compare_encoder" + File.separator + inputWsqFile);
    assertThat(inputPath).isNotEmptyFile();

    String dirname = "decode_encode";
    Path resultPgmPath = getTestPath(dirname, inputPath, Pair.of(".wsq", ".pgm"));
    Path outputWsqPath = getTestPath(dirname, resultPgmPath, Pair.of(".pgm", "-result.wsq"));

    // When decode
    DecodeToBufferedHelper.decodeWSQ(inputPath, resultPgmPath);
    assertThat(resultPgmPath).isNotEmptyFile();

    // When encode
    EncodeFromBufferedHelper.encodeWSQ(resultPgmPath, 225, outputWsqPath);
    assertThat(outputWsqPath).isNotEmptyFile();

    // Then analyze
    Path reportCompareFilePath = getTestPath(dirname, inputPath, Pair.of(".wsq", "-report.txt"));
    log.info(
        "compare original({}) with decode/encode({}) and report to {}",
        inputPath.getFileName(),
        outputWsqPath.getFileName(),
        reportCompareFilePath.getFileName());
    try (DataInputStream inputWsqIS = new DataInputStream(Files.newInputStream(inputPath));
        DataInputStream outputWsqIs = new DataInputStream(Files.newInputStream(outputWsqPath));
        Writer writer = Files.newBufferedWriter(reportCompareFilePath)) {
      WSQFingerprint inputWsqFp = WSQFingerprint.build(inputWsqIS);
      WSQFingerprint outputWsqFp = WSQFingerprint.build(outputWsqIs);

      writer.write(inputPath.getFileName() + "\n");
      writer.write(inputWsqFp + "\n");
      writer.write(outputWsqPath.getFileName() + "\n");
      writer.write(outputWsqFp + "\n");
      writer.write(
          "original ("
              + inputPath.getFileName()
              + ") vs decode/encode ("
              + outputWsqPath.getFileName()
              + ")\n");
      writer.write("-------------------------\n");
      boolean isEquals = WSQAnalyse.compareFingerprints(inputWsqFp, outputWsqFp);
      if (!isEquals) {
        WSQAnalyse.showHistograms(inputWsqFp, outputWsqFp, writer);
      }
    }
  }
}
