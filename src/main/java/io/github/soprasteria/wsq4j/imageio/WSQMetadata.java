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

import static io.github.soprasteria.wsq4j.domain.use_cases.helpers.coder.WSQEncoder.UNKNOW_PPI;

import io.github.soprasteria.wsq4j.domain.exceptions.Wsq4jInvalidArgumentException;
import java.util.*;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Node;

@Slf4j
public class WSQMetadata extends IIOMetadata {
  public static final String KEY_PPI = "PPI";
  public static final String KEY_BITRATE = "WSQ_BITRATE";

  Map<String, String> nistcom = new LinkedHashMap<>();

  @Getter final List<String> comments = new ArrayList<>();

  public WSQMetadata() {
    super(
        true,
        WSQMetadataFormat.getNativeMetadataFormatName(),
        WSQMetadataFormat.class.getName(),
        null,
        null);
    reset();
  }

  @SuppressWarnings("unused")
  public WSQMetadata(final int ppi, final double bitRate) {
    this();
    setProperty(KEY_PPI, Integer.toString(ppi));
    setProperty(KEY_BITRATE, Double.toString(bitRate));
  }

  public int getPPI() {
    if (!nistcom.containsKey(KEY_PPI)) {
      return UNKNOW_PPI;
    } else {
      return Integer.parseInt(nistcom.get(KEY_PPI));
    }
  }

  public double getBitrate() {
    if (!nistcom.containsKey(KEY_BITRATE)) {
      return Double.NaN;
    } else {
      return Double.parseDouble(nistcom.get(KEY_BITRATE));
    }
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public Node getAsTree(String formatName) {
    if (formatName.equals(IIOMetadataFormatImpl.standardMetadataFormatName)) {
      return getStandardTree();
    } else if (formatName.equals(nativeMetadataFormatName)) {
      return getNativeTree();
    } else {
      throw new Wsq4jInvalidArgumentException("Not a recognized format!");
    }
  }

  protected IIOMetadataNode getNativeTree() {
    final IIOMetadataNode root = new IIOMetadataNode(nativeMetadataFormatName);
    for (final Map.Entry<String, String> entry : nistcom.entrySet()) {
      final IIOMetadataNode node = new IIOMetadataNode("property");
      node.setAttribute("name", entry.getKey());
      node.setAttribute("value", entry.getValue());
      root.appendChild(node);
    }
    return root;
  }

  @Override
  public void reset() {
    nistcom.clear();
  }

  public String getNistcom() {
    final StringBuilder ret = new StringBuilder();
    ret.append("NIST_COM ").append(nistcom.size()).append("\n");
    for (final Map.Entry<String, String> entry : nistcom.entrySet()) {
      ret.append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
    }
    return ret.toString();
  }

  @SuppressWarnings("unused")
  public String getProperty(final String key) {
    return nistcom.get(key);
  }

  public void setProperty(final String key, String value) {
    if (value == null || value.isEmpty()) {
      nistcom.remove(key);
    } else {
      nistcom.put(key, value);
    }
  }

  @Override
  protected IIOMetadataNode getStandardDimensionNode() {
    final IIOMetadataNode dimension = new IIOMetadataNode("Dimension");

    final IIOMetadataNode aspect_node = new IIOMetadataNode("PixelAspectRatio");
    aspect_node.setAttribute("value", Double.toString(1));
    dimension.appendChild(aspect_node);

    if (nistcom.containsKey(KEY_PPI)) {
      final String pixSizeStr = Double.toString(25.4 / getPPI());
      final IIOMetadataNode pixelSizeX = new IIOMetadataNode("HorizontalPixelSize");
      pixelSizeX.setAttribute("value", pixSizeStr);
      dimension.appendChild(pixelSizeX);

      final IIOMetadataNode pixelSizeY = new IIOMetadataNode("VerticalPixelSize");
      pixelSizeY.setAttribute("value", pixSizeStr);
      dimension.appendChild(pixelSizeY);

      final IIOMetadataNode pixelPhysSizeX = new IIOMetadataNode("HorizontalPhysicalPixelSpacing");
      pixelPhysSizeX.setAttribute("value", pixSizeStr);
      dimension.appendChild(pixelPhysSizeX);

      final IIOMetadataNode pixelPhysSizeY = new IIOMetadataNode("VerticalPhysicalPixelSpacing");
      pixelPhysSizeY.setAttribute("value", pixSizeStr);
      dimension.appendChild(pixelPhysSizeY);
    }

    return dimension;
  }

  @Override
  protected IIOMetadataNode getStandardChromaNode() {
    final IIOMetadataNode chroma = new IIOMetadataNode("Chroma");

    final IIOMetadataNode colorspace = new IIOMetadataNode("ColorSpaceType");
    colorspace.setAttribute("name", "GRAY");
    chroma.appendChild(colorspace);

    return chroma;
  }

  @Override
  protected IIOMetadataNode getStandardCompressionNode() {
    final IIOMetadataNode compression = new IIOMetadataNode("Compression");

    final IIOMetadataNode compressionName = new IIOMetadataNode("CompressionTypeName");
    compressionName.setAttribute("value", "WSQ");
    compression.appendChild(compressionName);

    final IIOMetadataNode lossless = new IIOMetadataNode("Lossless");
    lossless.setAttribute("value", "FALSE");
    compression.appendChild(lossless);

    if (nistcom.containsKey(KEY_BITRATE)) {
      final IIOMetadataNode bitrate = new IIOMetadataNode("BitRate");
      bitrate.setAttribute("value", nistcom.get(KEY_BITRATE));
      compression.appendChild(bitrate);
    }
    return compression;
  }

  @Override
  public void setFromTree(final String formatName, final Node root) throws IIOInvalidTreeException {
    final Map<String, String> backup = new LinkedHashMap<>(nistcom);
    boolean success = false;
    try {
      reset();
      mergeTree(formatName, root);
      success = true;
    } finally {
      if (!success) nistcom = backup;
    }
  }

  @Override
  public void mergeTree(final String formatName, final Node root) throws IIOInvalidTreeException {
    final Map<String, String> backup = new LinkedHashMap<>(nistcom);
    boolean success = false;
    try {
      if (root == null) {
        throw new Wsq4jInvalidArgumentException("root == null!");
      }

      if (formatName.equals(IIOMetadataFormatImpl.standardMetadataFormatName)) {
        mergeStandardTree(root);
      } else if (formatName.equals(nativeMetadataFormatName)) {
        mergeNativeTree(root);
      } else {
        throw new Wsq4jInvalidArgumentException("Not a recognized format!");
      }
      success = true;
    } finally {
      if (!success) nistcom = backup;
    }
  }

  protected void mergeNativeTree(final Node root) throws IIOInvalidTreeException {
    if (!nativeMetadataFormatName.equals(root.getNodeName()))
      throw new IIOInvalidTreeException("Root node should be a " + nativeMetadataFormatName, root);

    for (Node property = root.getFirstChild();
        property != null;
        property = property.getNextSibling()) {
      if (!"property".equals(property.getNodeName()))
        throw new IIOInvalidTreeException("Expected 'property' element", property);

      String propertyName = null;
      String propertyValue = null;
      for (int i = 0; i < property.getAttributes().getLength(); i++) {
        final Node attribute = property.getAttributes().item(i);
        if ("name".equals(attribute.getNodeName())) {
          propertyName = attribute.getNodeValue();
        } else if ("value".equals(attribute.getNodeName())) {
          propertyValue = attribute.getNodeValue();
        } else {
          throw new IIOInvalidTreeException(
              "Invalid attribute '" + attribute.getNodeName() + "', should be 'name' or 'value'",
              attribute);
        }
      }

      if (propertyName == null)
        throw new IIOInvalidTreeException("Property does not have the 'name' attribute", property);

      try {
        setProperty(propertyName, propertyValue);
      } catch (final Throwable t) {
        throw new IIOInvalidTreeException(t.toString(), property);
      }
    }
  }

  protected void mergeStandardTree(final Node root) throws IIOInvalidTreeException {
    if (!IIOMetadataFormatImpl.standardMetadataFormatName.equals(root.getNodeName()))
      throw new IIOInvalidTreeException(
          "Expected '" + IIOMetadataFormatImpl.standardMetadataFormatName + "' element", root);

    for (Node element = root.getFirstChild(); element != null; element = element.getNextSibling()) {
      if ("Compression".equals(element.getNodeName())) mergeStandardCompressionNode(element);
      if ("Dimension".equals(element.getNodeName())) mergeStandardDimensionNode(element);
    }
  }

  protected void mergeStandardCompressionNode(final Node compression)
      throws IIOInvalidTreeException {
    if (!"Compression".equals(compression.getNodeName()))
      throw new IIOInvalidTreeException("Expected 'Compression' element", compression);

    for (Node bitrate = compression.getFirstChild();
        bitrate != null;
        bitrate = bitrate.getNextSibling()) {
      if (!"BitRate".equals(bitrate.getNodeName())) continue;

      for (int i = 0; i < bitrate.getAttributes().getLength(); i++) {
        final Node attribute = bitrate.getAttributes().item(i);
        if ("value".equals(attribute.getNodeName())) {
          final String bitrate_value = attribute.getNodeValue();
          setProperty(KEY_BITRATE, bitrate_value);
        }
      }
    }
  }

  protected void mergeStandardDimensionNode(final Node dimensionNode)
      throws IIOInvalidTreeException {
    double pixelSizeX = Double.NaN;
    double pixelSizeY = Double.NaN;
    double pixelPhysicalSizeX = Double.NaN;
    double pixelPhysicalSizeY = Double.NaN;
    double aspectRatio = 1;

    if (!"Dimension".equals(dimensionNode.getNodeName()))
      throw new IIOInvalidTreeException("Expected 'Dimension' element", dimensionNode);

    for (Node pixelSizeNode = dimensionNode.getFirstChild();
        pixelSizeNode != null;
        pixelSizeNode = pixelSizeNode.getNextSibling()) {
      // Skip the node if we cannot use it.
      if (!"PixelAspectRatio".equals(pixelSizeNode.getNodeName())
          && !"HorizontalPhysicalPixelSpacing".equals(pixelSizeNode.getNodeName())
          && !"VerticalPhysicalPixelSpacing".equals(pixelSizeNode.getNodeName())
          && !"HorizontalPixelSize".equals(pixelSizeNode.getNodeName())
          && !"VerticalPixelSize".equals(pixelSizeNode.getNodeName())) {
        continue;
      }

      double value = Double.NaN;
      for (int i = 0; i < pixelSizeNode.getAttributes().getLength(); i++) {
        final Node valueNode = pixelSizeNode.getAttributes().item(i);
        if (!"value".equals(valueNode.getNodeName())) continue;
        try {
          value = Double.parseDouble(valueNode.getNodeValue());
        } catch (final Throwable t) {
          log.debug("Ignores NumberFormatException, will be handled bellow");
        }
      }
      if (!(value > 0))
        throw new IIOInvalidTreeException(
            "Empty or Invalid " + pixelSizeNode.getNodeName(), pixelSizeNode);

      if ("PixelAspectRatio".equals(pixelSizeNode.getNodeName())) aspectRatio = value;

      if ("HorizontalPhysicalPixelSpacing".equals(pixelSizeNode.getNodeName()))
        pixelPhysicalSizeX = value;

      if ("VerticalPhysicalPixelSpacing".equals(pixelSizeNode.getNodeName()))
        pixelPhysicalSizeY = value;

      if ("HorizontalPixelSize".equals(pixelSizeNode.getNodeName())) pixelSizeX = value;

      if ("VerticalPixelSize".equals(pixelSizeNode.getNodeName())) pixelSizeY = value;
    }

    if (!Double.isNaN(pixelPhysicalSizeX) || !Double.isNaN(pixelPhysicalSizeY)) {
      if (Double.isNaN(pixelPhysicalSizeX)) pixelPhysicalSizeX = pixelPhysicalSizeY * aspectRatio;
      if (Double.isNaN(pixelPhysicalSizeY)) pixelPhysicalSizeY = pixelPhysicalSizeX / aspectRatio;
      final double pixelSize = Math.sqrt(pixelPhysicalSizeX * pixelPhysicalSizeY);
      setProperty(KEY_PPI, Double.toString(25.4 / pixelSize));

    } else if (!Double.isNaN(pixelSizeX) || !Double.isNaN(pixelSizeY)) {
      if (Double.isNaN(pixelSizeX)) pixelSizeX = pixelSizeY * aspectRatio;
      if (Double.isNaN(pixelSizeY)) pixelSizeY = pixelSizeX / aspectRatio;
      final double pixelSize = Math.sqrt(pixelSizeX * pixelSizeY);
      setProperty(KEY_PPI, Double.toString(25.4 / pixelSize));
    }
  }

  @Override
  public String toString() {
    return "WSQ metadata: " + nistcom;
  }

  public void addComment(final String s) {
    if (s != null) comments.add(s);
  }
}
