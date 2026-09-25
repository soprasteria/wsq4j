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

import static io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder.WSQEncoder.*;

import io.github.soprasteria.wsq4j.domain.entities.Bitmap;
import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jEncodeException;
import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jInvalidArgumentException;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.ConvertImageToGrayscaleHelper;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.Wsq4jConfig;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder.WSQEncoder;
import java.awt.image.BufferedImage;
import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WSQImageWriter extends ImageWriter {
  public WSQImageWriter(ImageWriterSpi provider) {
    super(provider);
  }

  public ImageWriteParam getDefaultWriteParam() {
    return new WSQImageWriteParam(DEFAULT_BITRATE, DEFAULT_PPI);
  }

  public IIOMetadata convertImageMetadata(
      final IIOMetadata inData, final ImageTypeSpecifier imageType, final ImageWriteParam param) {
    return null;
  }

  public IIOMetadata convertStreamMetadata(final IIOMetadata inData, final ImageWriteParam param) {
    if (inData instanceof WSQMetadata) {
      return inData;
    }
    return null;
  }

  public IIOMetadata getDefaultImageMetadata(
      final ImageTypeSpecifier imageType, final ImageWriteParam param) {
    return new WSQMetadata();
  }

  public IIOMetadata getDefaultStreamMetadata(final ImageWriteParam param) {
    return null;
  }

  @Override
  public void setOutput(Object output) {
    super.setOutput(output);
    if (output != null) {
      if (!(output instanceof ImageOutputStream)) {
        throw new Wsq4jInvalidArgumentException("output not an ImageOutputStream!");
      }
    }
  }

  public void write(
      final IIOMetadata streamMetaData, final IIOImage image, final ImageWriteParam param)
      throws IIOException {
    try {
      double bitRate = DEFAULT_BITRATE;
      int ppi = DEFAULT_PPI;

      WSQMetadata metadata = (WSQMetadata) image.getMetadata();
      if (metadata == null) {
        metadata = new WSQMetadata();
      }
      if (metadata.getPPI() > 0) {
        ppi = metadata.getPPI();
      }
      if (!Double.isNaN(metadata.getBitrate())) {
        bitRate = metadata.getBitrate();
      }
      if (param instanceof WSQImageWriteParam) {
        WSQImageWriteParam wsqParam = (WSQImageWriteParam) param;
        if (!Double.isNaN(wsqParam.getBitRate())) {
          bitRate = wsqParam.getBitRate();
        }
        if (wsqParam.getPpi() > 0) {
          ppi = wsqParam.getPpi();
        }
      }
      final BufferedImage bufferedImage =
          ConvertImageToGrayscaleHelper.execute((BufferedImage) image.getRenderedImage());
      if (!(output instanceof ImageOutputStream)) {
        throw new Wsq4jEncodeException("bad output");
      }

      final int width = bufferedImage.getWidth();
      final int height = bufferedImage.getHeight();
      log.debug("PPI:{}, BITRATE:{}, width:{}, height:{}", ppi, bitRate, width, height);
      byte[] imageBytes =
          (byte[]) bufferedImage.getRaster().getDataElements(0, 0, width, height, null);
      final Bitmap bitmap =
          new Bitmap(imageBytes, width, height, ppi, DEFAULT_DEEPTH, LOSSYFLAG_WITH_LOSS);
      WSQEncoder.encode(
          (ImageOutputStream) getOutput(),
          bitmap,
          bitRate,
          metadata.nistcom,
          Wsq4jConfig.WSQ_COMMENT);
    } catch (final Throwable t) {
      throw new IIOException(t.getMessage(), t);
    }
  }
}
