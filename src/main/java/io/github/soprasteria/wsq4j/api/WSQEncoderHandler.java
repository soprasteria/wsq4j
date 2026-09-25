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
package io.github.soprasteria.wsq4j.api;

import static io.github.soprasteria.wsq4j.domain.constants.WSQConstants.DEFAULT_DEEPTH;
import static io.github.soprasteria.wsq4j.domain.constants.WSQConstants.LOSSYFLAG_WITH_LOSS;

import io.github.soprasteria.wsq4j.domain.entities.Bitmap;
import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jEncodeException;
import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jException;
import io.github.soprasteria.wsq4j.domain.use_cases.helpers.ConvertImageToGrayscaleHelper;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("unused")
@Slf4j
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class WSQEncoderHandler {
  private int ppi;
  private double bitRate;
  private String comments;

  public WsqHandler encode(String fileName) {
    return encode(new File(fileName));
  }

  public WsqHandler encode(File file) {
    try {
      BufferedImage inputImage = ImageIO.read(file);
      Bitmap bitmap = convertToBitmap(inputImage);
      return new WsqHandler(bitmap, bitRate, comments);
    } catch (Exception e) {
      throw new Wsq4jEncodeException(e);
    }
  }

  private Bitmap convertToBitmap(BufferedImage inputImage) {
    BufferedImage grayImage = ConvertImageToGrayscaleHelper.execute(inputImage);
    byte[] grayBytes = ((DataBufferByte) grayImage.getRaster().getDataBuffer()).getData();
    return new Bitmap(
        grayBytes,
        grayImage.getWidth(),
        grayImage.getHeight(),
        ppi,
        DEFAULT_DEEPTH,
        LOSSYFLAG_WITH_LOSS);
  }

  public WsqHandler encode(InputStream inputStream) {
    try {
      BufferedImage inputImage = ImageIO.read(inputStream);
      Bitmap bitmap = convertToBitmap(inputImage);
      return new WsqHandler(bitmap, bitRate, comments);
    } catch (Exception e) {
      throw new Wsq4jEncodeException(e);
    }
  }

  public WsqHandler encode(final byte[] data, @NonNull String format) {
    ImageReader firstFormatReader;
    try {
      firstFormatReader =
          Optional.ofNullable(ImageIO.getImageReadersByFormatName(format))
              .map(Iterator::next)
              .orElseThrow(
                  () ->
                      new Wsq4jEncodeException("Unable to find ImageReader for format :" + format));

    } catch (NoSuchElementException e) {
      throw new Wsq4jEncodeException("Unknow format " + format, e);
    }

    InputStream input = new ByteArrayInputStream(data);
    try (ImageInputStream iis = ImageIO.createImageInputStream(input)) {
      firstFormatReader.setInput(iis, false, true);
      int nbImage = firstFormatReader.getNumImages(true);
      if (nbImage > 1) {
        log.warn("Multiple images {} found for format but only the first will be encode", nbImage);
      }
      if (nbImage == 0) {
        throw new Wsq4jEncodeException("No image founded");
      }
      BufferedImage bufferedImage = firstFormatReader.read(0);
      firstFormatReader.dispose();

      Bitmap bitmap = convertToBitmap(bufferedImage);
      return new WsqHandler(bitmap, bitRate, comments);
    } catch (Wsq4jException w4j) {
      throw w4j;
    } catch (Exception e) {
      throw new Wsq4jEncodeException("Exception while encode", e);
    }
  }
}
