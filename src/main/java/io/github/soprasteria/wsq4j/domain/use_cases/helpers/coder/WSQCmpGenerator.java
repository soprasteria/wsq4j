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
package io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder;

import static io.github.soprasteria.wsq4j.domain.constants.WSQConstants.NUM_SUBBANDS;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WSQCmpGenerator {

  @SuppressWarnings("ClassEscapesDefinedScope")
  public static void generate(
      Path analyseDirPath, String filename, int[] qdata, WSQHelper.QuantTree[] qtree)
      throws IOException {
    deleteDirectoryIfExists(analyseDirPath);
    Files.createDirectories(analyseDirPath);

    int index = 0;
    for (int cnt = 0; cnt < NUM_SUBBANDS; cnt++) {
      Path cmpFile = Paths.get(analyseDirPath.toString(), String.format("%s.%02d", filename, cnt));

      try (OutputStream outputStream = Files.newOutputStream(cmpFile);
          PrintWriter out = new PrintWriter(outputStream)) {

        int bandlength = qtree[cnt].lenx * qtree[cnt].leny;
        for (int i = 0; i < bandlength && index < qdata.length; i += 10) {

          out.printf("%5d:", i);
          for (int j = 0; j < 10 && i + j < bandlength && index < qdata.length; j++) {
            out.printf("%5d", qdata[index++]);
          }
          out.println();
        }
      }
    }
    log.info("Analyse directory : {}", analyseDirPath.toAbsolutePath());
  }

  public static void deleteDirectoryIfExists(Path dir) throws IOException {
    if (Files.exists(dir)) {

      try (Stream<Path> stream = Files.walk(dir)) {
        stream
            .sorted(Comparator.reverseOrder())
            .forEach(
                path -> {
                  try {
                    Files.deleteIfExists(path);
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                });
      }
    }
  }
}
