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
package io.github.soprasteria.wsq4j.imageio;

import java.util.Locale;
import javax.imageio.ImageWriteParam;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WSQImageWriteParam extends ImageWriteParam {
  private double bitRate;
  private int ppi;

  public WSQImageWriteParam(double bitRate, final int ppi) {
    super(Locale.getDefault());
    this.bitRate = bitRate;
    this.ppi = ppi;
  }
}
