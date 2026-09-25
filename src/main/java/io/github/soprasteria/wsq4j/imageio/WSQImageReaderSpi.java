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

import io.github.soprasteria.wsq4j.domain.use_cases.helpers.WSQCheckMagicByte;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

public class WSQImageReaderSpi extends ImageReaderSpi implements WSQImageInfoConstants {

  public static final String readerClassName = WSQImageReader.class.getName();
  static final String[] writerSpiNames = {};
  static final boolean supportsStandardStreamMetadataFormat = false;
  static final String nativeStreamMetadataFormatName = null;
  static final String nativeStreamMetadataFormatClassName = null;
  static final String[] extraStreamMetadataFormatNames = null;
  static final String[] extraStreamMetadataFormatClassNames = null;
  static final boolean supportsStandardImageMetadataFormat = true;
  static final String[] extraImageMetadataFormatNames = null;
  static final String[] extraImageMetadataFormatClassNames = null;

  public WSQImageReaderSpi() {

    super(
        wsqVendorName,
        wsqSoftwareVersion,
        wsqFormatNames,
        wsqSuffixes,
        wsqMimeTypes,
        readerClassName,
        STANDARD_INPUT_TYPE,
        writerSpiNames,
        supportsStandardStreamMetadataFormat,
        nativeStreamMetadataFormatName,
        nativeStreamMetadataFormatClassName,
        extraStreamMetadataFormatNames,
        extraStreamMetadataFormatClassNames,
        supportsStandardImageMetadataFormat,
        wsqNativeImageMetadataFormatName,
        wsqNativeImageMetadataFormatClassName,
        extraImageMetadataFormatNames,
        extraImageMetadataFormatClassNames);
  }

  @Override
  public boolean canDecodeInput(Object input) throws IOException {
    if (input instanceof ImageInputStream) {
      ImageInputStream inStream = (ImageInputStream) input;
      inStream.mark();
      int magicByteHeader = inStream.readUnsignedShort();
      inStream.reset();
      return WSQCheckMagicByte.execute(magicByteHeader);
    } else {
      return false;
    }
  }

  @Override
  public ImageReader createReaderInstance(Object extension) {
    return new WSQImageReader(this);
  }

  @Override
  public String getDescription(Locale locale) {
    return wsqDescription;
  }
}
