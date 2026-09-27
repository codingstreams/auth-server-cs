package com.example.authserver.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public interface RsaKeyLoader {

  static RSAPrivateKey loadPrivateKey(String filePath) throws Exception {
    String keyContent = readKeyContent(filePath);
    String cleanedKey = cleanPemContent(keyContent);
    byte[] keyBytes = Base64.getDecoder().decode(cleanedKey);
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
  }

  static RSAPublicKey loadPublicKey(String filePath) throws Exception {
    String keyContent = readKeyContent(filePath);
    String cleanedKey = cleanPemContent(keyContent);
    byte[] keyBytes = Base64.getDecoder().decode(cleanedKey);
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    return (RSAPublicKey) keyFactory.generatePublic(keySpec);
  }

  private static String readKeyContent(String filePath) throws IOException {
    if (filePath == null || filePath.isBlank()) {
      throw new IllegalArgumentException("Key file path cannot be null or blank");
    }

    if (filePath.startsWith("classpath:")) {
      String resourcePath = filePath.substring("classpath:".length()).trim();
      if (resourcePath.startsWith("/")) {
        resourcePath = resourcePath.substring(1);
      }
      try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
        if (is == null) {
          throw new FileNotFoundException("Classpath resource not found: " + filePath);
        }
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
      }
    }

    Path path = Path.of(filePath);
    if (Files.exists(path)) {
      return Files.readString(path, StandardCharsets.UTF_8);
    }

    String cleanPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
    try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(cleanPath)) {
      if (is != null) {
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
      }
    }

    throw new FileNotFoundException("Key file not found at path: " + filePath);
  }

  private static String cleanPemContent(String pem) {
    return pem.replaceAll("-----BEGIN [^-]+-----", "")
        .replaceAll("-----END [^-]+-----", "")
        .replaceAll("\\s+", "");
  }
}
