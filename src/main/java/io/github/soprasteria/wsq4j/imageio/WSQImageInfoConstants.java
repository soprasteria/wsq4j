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

public interface WSQImageInfoConstants {
  String wsqVendorName = "Sopra Steria Group";
  String wsqSoftwareVersion = "1.0.0";
  String[] wsqFormatNames = {"wsq", "WSQ", "WSQ FBI"};
  String[] wsqSuffixes = {"wsq"};
  String[] wsqMimeTypes = {"image/x-wsq"};
  String classPackage = "io.github.soprasteria.wsq4j.imageio";
  String wsqNativeImageMetadataFormatName = classPackage + ".WSQMetadata_1.0";
  String wsqNativeImageMetadataFormatClassName = classPackage + ".WSQMetadata";
  String wsqDescription = "Wavelet Scalar Quantization (WSQ)";
}
