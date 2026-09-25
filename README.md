# wsq4j

Java implementation for encoding, decoding WSQ format of image files

## Usage

### Using by ImageIO

- Read
```java
BufferedImage wsqImage = ImageIO.read(new File("src/test/resources/nbis/ref-finger.wsq"));
```

- Write
```java
BufferedImage imgInput = ImageIO.read(new File("src/test/resources/nbis/finger.png"));
ImageIO.write(imgInput, "wsq", new File("target/test-default.wsq"));
```

- Write with specify compression and ppi
```java
BufferedImage imgInput = ImageIO.read(new File("src/test/resources/nbis/finger.png"));

ImageWriter writer = ImageIO.getImageWritersByFormatName("wsq").next();
WSQImageWriteParam wsqParams = new WSQImageWriteParam(2.25, 1000);
try(ImageOutputStream ios = ImageIO.createImageOutputStream(new File("target/test-1000ppi-2.25bitrate.wsq"))) {
  writer.setOutput(ios);
  writer.write(null, new javax.imageio.IIOImage(imgInput, null, null), wsqParams);
  writer.dispose();
}
```

### Using wsq4j's functional API

- Decode
```java
Wsq4j.decoder()
    .decode(new File("src/test/resources/nbis/ref-finger.wsq"))
    .asPng()
    .asFile("target/test-wsq4japi_decode.png");
```

- Encode
```java
Wsq4j.encoder(500, 0.75)
    .encode(new File("src/test/resources/nbis/finger.png"))
    .asFile(new File("target/test-wsq4japi_encode.png"));
```

## Inception

Some parts of this framework are derived from or inspired by the NBIS (https://github.com/lessandro/nbis) source code,
originally developed by NIST and released into the public domain.
Some of these parts have been ported from C to Java.

## Copyright & Licensing

Copyright 2026, Sopra Steria Group

This project is licensed under the Apache License, version 2.0.
The full text of the license is available in the LICENSE file.
