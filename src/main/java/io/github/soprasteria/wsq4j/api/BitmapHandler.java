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

import io.github.soprasteria.wsq4j.domain.entities.Bitmap;
import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jException;
import java.awt.image.*;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import lombok.NonNull;

@SuppressWarnings("unused")
public class BitmapHandler {
  private static final int[] MASKS = {0x000000ff, 0x000000ff, 0x000000ff};

  private final Bitmap bitmap;

  public BitmapHandler(Bitmap bitmap) {
    this.bitmap = bitmap;
  }

  public FileHandler toPng() {
    return new FileHandler(convert(bitmap, "png"));
  }

  public FileHandler toGif() {
    return new FileHandler(convert(bitmap, "gif"));
  }

  public FileHandler toJpg() {
    return new FileHandler(convert(bitmap, "jpeg"));
  }

  public Bitmap asBitmap() {
    return bitmap;
  }

  private byte[] convert(@NonNull Bitmap bitmap, @NonNull String format) {
    final int width = bitmap.getWidth();
    final int height = bitmap.getHeight();

    DataBuffer buffer = new DataBufferByte(bitmap.getPixels(), bitmap.getLength());
    WritableRaster writableRaster =
        Raster.createPackedRaster(buffer, width, height, width, MASKS, null);
    BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    bufferedImage.setData(writableRaster);

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      ImageIO.write(bufferedImage, format, outputStream);
      return outputStream.toByteArray();
    } catch (Exception e) {
      throw new Wsq4jException("Exception while converting to " + format, e);
    }
  }
}
