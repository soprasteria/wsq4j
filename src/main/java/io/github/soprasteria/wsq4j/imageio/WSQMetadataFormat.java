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

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import lombok.Getter;

public class WSQMetadataFormat extends IIOMetadataFormatImpl implements WSQImageInfoConstants {
  @Getter private static final IIOMetadataFormat instance = new WSQMetadataFormat();
  @Getter private static final String nativeMetadataFormatName = wsqNativeImageMetadataFormatName;

  public WSQMetadataFormat() {
    super(nativeMetadataFormatName, CHILD_POLICY_SEQUENCE);

    addElement("property", nativeMetadataFormatName, CHILD_POLICY_EMPTY);
    addAttribute("property", "name", DATATYPE_STRING, true, null);
    addAttribute("property", "value", DATATYPE_STRING, false, null);

    addElement("comment", nativeMetadataFormatName, CHILD_POLICY_EMPTY);
    addAttribute("comment", "value", DATATYPE_STRING, false, null);
  }

  @Override
  public boolean canNodeAppear(final String elementName, final ImageTypeSpecifier imageType) {
    return true;
  }
}
