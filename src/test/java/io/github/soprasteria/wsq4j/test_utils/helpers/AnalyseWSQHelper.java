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

import io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder.WSQAnalyse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AnalyseWSQHelper {

  public static void analyseWSQ(Path wsqPath, Path analyseDir) throws IOException {

    try (InputStream inputStream = Files.newInputStream(wsqPath)) {
      String filename = wsqPath.getFileName().toString().replace(".wsq", "_wsq_analyse");
      Path analyseDirPath = Paths.get(analyseDir.toString(), filename);

      WSQAnalyse.analyse(inputStream, analyseDirPath, wsqPath.getFileName().toString());
    }
  }
}
