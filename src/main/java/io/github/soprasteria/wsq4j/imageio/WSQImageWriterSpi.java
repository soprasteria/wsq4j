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
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

public class WSQImageWriterSpi extends ImageWriterSpi implements WSQImageInfoConstants {

  static final boolean supportsStandardStreamMetadataFormat = false;
  static final String nativeStreamMetadataFormatName = null;
  static final String nativeStreamMetadataFormatClassName = null;
  static final String[] extraStreamMetadataFormatNames = null;
  static final String[] extraStreamMetadataFormatClassNames = null;
  static final boolean supportsStandardImageMetadataFormat = true;
  static final String[] extraImageMetadataFormatNames = null;
  static final String[] extraImageMetadataFormatClassNames = null;
  private static final String writerClassName = WSQImageWriter.class.getName();
  static final String[] readerSpiNames = {WSQImageReaderSpi.class.getName()};

  public WSQImageWriterSpi() {
    super(
        wsqVendorName,
        wsqSoftwareVersion,
        wsqFormatNames,
        wsqSuffixes,
        wsqMimeTypes,
        writerClassName,
        new Class[] {ImageOutputStream.class},
        readerSpiNames,
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
  public boolean canEncodeImage(ImageTypeSpecifier type) {
    return true;
  }

  @Override
  public ImageWriter createWriterInstance(Object extension) {
    return new WSQImageWriter(this);
  }

  @Override
  public String getDescription(Locale locale) {
    return wsqDescription;
  }
}
