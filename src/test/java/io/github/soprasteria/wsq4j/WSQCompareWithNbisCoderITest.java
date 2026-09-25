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

import static io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder.WSQEncoder.*;
import static io.github.soprasteria.wsq4j.test_utils.fixtures.FixturesUtils.OUTPUT_TEST_DIRECTORY;
import static io.github.soprasteria.wsq4j.test_utils.fixtures.FixturesUtils.getTestPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.soprasteria.wsq4j.domain.entities.Bitmap;
import io.github.soprasteria.wsq4j.domain.entities.Pair;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.ReadAllBytesHelper;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.Wsq4jConfig;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder.WSQAnalyse;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder.WSQEncoder;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder.WSQFingerprint;
import io.github.soprasteria.wsq4j.test_utils.helpers.AnalyseWSQHelper;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WSQCompareWithNbisCoderITest {

  private final String dirname = "compare_with_nbis";

  @Order(10)
  @Test
  void encode_should_transform_raw_to_wsq() throws IOException {
    // Given
    Path rawInputPath = Paths.get("src/test/resources/nbis/finger.raw");
    Path wsqOuputPath = getTestPath(dirname, rawInputPath, Pair.of(".raw", "-result.wsq"));

    // When
    try (DataInputStream rawIS = new DataInputStream(Files.newInputStream(rawInputPath));
        OutputStream wsqOS = Files.newOutputStream(wsqOuputPath)) {

      Bitmap bitmap =
          new Bitmap(
              ReadAllBytesHelper.execute(rawIS), 500, 500, 500, DEFAULT_DEEPTH, LOSSYFLAG_NO_LOSS);

      // When
      log.info("encode {} to {}", rawInputPath.getFileName().toString(), wsqOuputPath);
      WSQEncoder.encode(wsqOS, bitmap, 2.25, Wsq4jConfig.WSQ_COMMENT);
    }
    // Then
    assertThat(wsqOuputPath).isNotEmptyFile();
  }

  @Order(20)
  @Test
  void compare_should_be_identical_as_nbis() throws IOException {
    // Given
    Path rawInputPath = Paths.get("src/test/resources/nbis/finger.raw");
    Path resultWsqPath = getTestPath(dirname, rawInputPath, Pair.of(".raw", "-result.wsq"));
    Path refNbisWsqPath = Paths.get("src/test/resources/nbis/ref-finger.wsq");
    Path reportPath = getTestPath(dirname, resultWsqPath, Pair.of(".wsq", "-report.txt"));

    Path analyseDirname = Paths.get(OUTPUT_TEST_DIRECTORY, dirname);
    log.info("analyseWSQ {}", refNbisWsqPath);
    AnalyseWSQHelper.analyseWSQ(refNbisWsqPath, analyseDirname);
    log.info("analyseWSQ {}", resultWsqPath);
    AnalyseWSQHelper.analyseWSQ(resultWsqPath, analyseDirname);

    try (DataInputStream refNbisWsqIS = new DataInputStream(Files.newInputStream(refNbisWsqPath));
        DataInputStream resultWsqIS = new DataInputStream(Files.newInputStream(resultWsqPath));
        Writer writer = Files.newBufferedWriter(reportPath)) {

      WSQFingerprint fpRefWsq = WSQFingerprint.build(refNbisWsqIS);
      WSQFingerprint fpResultWsq = WSQFingerprint.build(resultWsqIS);

      writer.write(resultWsqPath.getFileName() + "\n");
      writer.write(fpResultWsq + "\n\n");
      writer.write(refNbisWsqPath.getFileName() + "\n");
      writer.write(fpRefWsq + "\n\n");
      writer.write(refNbisWsqPath.getFileName() + " vs " + resultWsqPath.getFileName() + "\n");
      writer.write("-------------------------\n");

      // When Then
      assertTrue(WSQAnalyse.compareFingerprints(fpRefWsq, fpResultWsq, writer));

      WSQAnalyse.showHistograms(fpResultWsq, fpRefWsq, writer);
    }
  }
}
