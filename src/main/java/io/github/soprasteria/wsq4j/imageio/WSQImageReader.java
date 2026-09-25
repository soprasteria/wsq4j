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

import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;

import io.github.soprasteria.wsq4j.domain.entities.BitmapWithMetadata;
import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jDecodeException;
import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jException;
import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jInvalidArgumentException;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder.WSQDecoder;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WSQImageReader extends ImageReader {
  private WSQMetadata metadata;
  private BufferedImage image;

  public WSQImageReader(final ImageReaderSpi provider) {
    super(provider);
  }

  @Override
  public int getNumImages(boolean allowSearch) {
    processInput(0);
    return 1;
  }

  @Override
  public int getWidth(int imageIndex) {
    processInput(imageIndex);
    return image.getWidth();
  }

  @Override
  public int getHeight(int imageIndex) {
    processInput(imageIndex);
    return image.getHeight();
  }

  @Override
  public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) {
    processInput(imageIndex);
    return Collections.singletonList(ImageTypeSpecifier.createFromRenderedImage(image)).iterator();
  }

  @Override
  public IIOMetadata getStreamMetadata() {
    return null;
  }

  @Override
  public IIOMetadata getImageMetadata(int imageIndex) {
    processInput(imageIndex);
    return metadata;
  }

  @Override
  public BufferedImage read(int imageIndex, ImageReadParam param) {
    processInput(imageIndex);
    return image;
  }

  private void processInput(final int imageIndex) {
    try {
      if (imageIndex != 0) {
        throw new IndexOutOfBoundsException("imageIndex " + imageIndex);
      }

      if (image != null) {
        return;
      }

      final Object input = getInput();
      if (input == null) {
        this.image = null;
        return;
      }
      if (!(input instanceof ImageInputStream)) {
        throw new Wsq4jInvalidArgumentException(
            "bad input: " + input.getClass().getCanonicalName());
      }
      ImageInputStream imageIS = (ImageInputStream) input;
      log.debug("Input:{}", imageIS);
      final BitmapWithMetadata bitmap = WSQDecoder.decode(imageIS);

      metadata = new WSQMetadata();

      for (Map.Entry<String, String> entry : bitmap.getMetadata().entrySet()) {
        metadata.setProperty(entry.getKey(), entry.getValue());
      }
      for (String s : bitmap.getComments()) {
        metadata.addComment(s);
      }

      int width = bitmap.getWidth();
      int height = bitmap.getHeight();
      image = new BufferedImage(width, height, TYPE_BYTE_GRAY);

      DataBufferByte imgDataBuffer = (DataBufferByte) image.getRaster().getDataBuffer();
      final byte[] imageData = imgDataBuffer.getData();

      System.arraycopy(bitmap.getPixels(), 0, imageData, 0, width * height);
    } catch (Wsq4jException w4e) {
      throw w4e;
    } catch (Exception e) {
      this.image = null;
      throw new Wsq4jDecodeException("Exception while decoding", e);
    }
  }
}
