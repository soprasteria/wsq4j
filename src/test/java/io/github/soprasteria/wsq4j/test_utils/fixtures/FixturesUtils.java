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
package io.github.soprasteria.wsq4j.test_utils.fixtures;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.github.soprasteria.wsq4j.domain.entities.Pair;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

@Slf4j
public class FixturesUtils {
  public static final String OUTPUT_TEST_DIRECTORY = "target/results_tests";

  static {
    applyLogDebugIfRunningInDebugMode();
  }

  private static void applyLogDebugIfRunningInDebugMode() {
    if (ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("jdwp")) {
      Logger logger = (Logger) LoggerFactory.getLogger("io.github.soprasteria.wsq4j");
      logger.setLevel(Level.DEBUG);
      logger.debug("Debug mode detected, log level set to DEBUG");
    }
  }

  public static Path getTestPath(
      String directoryName, @NonNull Path inputFile, Pair<String, String> replace)
      throws IOException {
    return getTestPath(directoryName, inputFile, Collections.singletonList(replace));
  }

  public static Path getTestPath(
      String directoryName, @NonNull Path inputFile, List<Pair<String, String>> replaceList)
      throws IOException {

    String outputDir = OUTPUT_TEST_DIRECTORY;
    if (directoryName != null) {
      outputDir += File.separator + directoryName;
    }

    String filename = inputFile.getFileName().toString();
    for (Pair<String, String> pair : replaceList) {
      filename = filename.replace(pair.getLeft(), pair.getRight());
    }
    assertThat(filename).isNotEqualTo(inputFile.getFileName().toString());

    String filepathStr = outputDir + File.separator + filename;
    assertThat(filepathStr).contains("target");

    Path filepath = Paths.get(filepathStr);
    Files.createDirectories(filepath.getParent());
    return filepath;
  }
}
