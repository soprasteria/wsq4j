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
package io.github.soprasteria.wsq4j.domain.use_cases.helpers;

import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Wsq4jConfig {

  private static final String PROPERTY_FILE_NAME = "wsq4j.properties";
  private static final int NO_SF_ID = 0;

  public static final int WSQ_SF_ID;
  public static final String WSQ_COMMENT;

  static {
    Properties properties = loadProperties();
    WSQ_SF_ID = tryReadIntOrElse(properties.getProperty("wsq-sf-id"), NO_SF_ID);
    String wsqComment = tryReadStringOrElse(properties.getProperty("wsq-comment"), "Wsq4j beta");
    if (WSQ_SF_ID == NO_SF_ID) {
      wsqComment += " - not certified";
    }
    WSQ_COMMENT = wsqComment;
    log.debug("wsq-sf-id:'{}' and wsq-comment:'{}'", WSQ_SF_ID, WSQ_COMMENT);
  }

  @SuppressWarnings("SameParameterValue")
  private static int tryReadIntOrElse(String strValue, int defaultValue) {
    try {
      return Integer.parseInt(strValue);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  @SuppressWarnings("SameParameterValue")
  private static String tryReadStringOrElse(String strValue, String defaultValue) {
    if (strValue == null || strValue.isEmpty()) {
      return defaultValue;
    } else {
      return strValue;
    }
  }

  private static Properties loadProperties() {
    Properties properties = new Properties();

    try (InputStream input =
        Wsq4jConfig.class.getClassLoader().getResourceAsStream(PROPERTY_FILE_NAME)) {

      if (input == null) {
        throw new Wsq4jException("File : '" + PROPERTY_FILE_NAME + "' unreachable");
      }

      properties.load(input);
      return properties;
    } catch (IOException e) {
      throw new Wsq4jException("Unable to read file : '" + PROPERTY_FILE_NAME + "'", e);
    }
  }
}
