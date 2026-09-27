package com.example.authserver.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RsaKeyLoaderTest {

  @TempDir
  Path tempDir;

  private Path privateKeyPath;
  private Path publicKeyPath;
  private KeyPair originalKeyPair;

  @BeforeEach
  void setUp() throws Exception {
    KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
    keyPairGen.initialize(2048);
    originalKeyPair = keyPairGen.generateKeyPair();

    // Write Private Key in PEM format (PKCS#8)
    String privateKeyBase64 = Base64.getMimeEncoder(64, new byte[]{'\n'})
        .encodeToString(originalKeyPair.getPrivate().getEncoded());
    String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" + privateKeyBase64 + "\n-----END PRIVATE KEY-----\n";
    privateKeyPath = tempDir.resolve("private.pem");
    Files.writeString(privateKeyPath, privateKeyPem);

    // Write Public Key in PEM format (X.509)
    String publicKeyBase64 = Base64.getMimeEncoder(64, new byte[]{'\n'})
        .encodeToString(originalKeyPair.getPublic().getEncoded());
    String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" + publicKeyBase64 + "\n-----END PUBLIC KEY-----\n";
    publicKeyPath = tempDir.resolve("public.pem");
    Files.writeString(publicKeyPath, publicKeyPem);
  }

  @Test
  @DisplayName("Should successfully load RSA private key from valid PEM file")
  void shouldLoadPrivateKeySuccessfully() throws Exception {
    RSAPrivateKey loadedPrivateKey = RsaKeyLoader.loadPrivateKey(privateKeyPath.toString());

    assertThat(loadedPrivateKey).isNotNull();
    assertThat(loadedPrivateKey.getAlgorithm()).isEqualTo("RSA");
    assertThat(loadedPrivateKey.getEncoded()).isEqualTo(originalKeyPair.getPrivate().getEncoded());
  }

  @Test
  @DisplayName("Should successfully load RSA public key from valid PEM file")
  void shouldLoadPublicKeySuccessfully() throws Exception {
    RSAPublicKey loadedPublicKey = RsaKeyLoader.loadPublicKey(publicKeyPath.toString());

    assertThat(loadedPublicKey).isNotNull();
    assertThat(loadedPublicKey.getAlgorithm()).isEqualTo("RSA");
    assertThat(loadedPublicKey.getEncoded()).isEqualTo(originalKeyPair.getPublic().getEncoded());
  }

  @Test
  @DisplayName("Should verify signature using loaded private and public keys")
  void shouldSignAndVerifyWithLoadedKeys() throws Exception {
    RSAPrivateKey loadedPrivateKey = RsaKeyLoader.loadPrivateKey(privateKeyPath.toString());
    RSAPublicKey loadedPublicKey = RsaKeyLoader.loadPublicKey(publicKeyPath.toString());

    byte[] data = "test-payload-to-sign".getBytes();

    Signature signer = Signature.getInstance("SHA256withRSA");
    signer.initSign(loadedPrivateKey);
    signer.update(data);
    byte[] signature = signer.sign();

    Signature verifier = Signature.getInstance("SHA256withRSA");
    verifier.initVerify(loadedPublicKey);
    verifier.update(data);

    assertThat(verifier.verify(signature)).isTrue();
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when private key path is null")
  void shouldThrowExceptionWhenPrivateKeyPathIsNull() {
    assertThatThrownBy(() -> RsaKeyLoader.loadPrivateKey(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Key file path cannot be null or blank");
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when public key path is null")
  void shouldThrowExceptionWhenPublicKeyPathIsNull() {
    assertThatThrownBy(() -> RsaKeyLoader.loadPublicKey(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Key file path cannot be null or blank");
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when path is blank")
  void shouldThrowExceptionWhenPathIsBlank() {
    assertThatThrownBy(() -> RsaKeyLoader.loadPrivateKey("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Key file path cannot be null or blank");

    assertThatThrownBy(() -> RsaKeyLoader.loadPublicKey("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Key file path cannot be null or blank");
  }

  @Test
  @DisplayName("Should throw FileNotFoundException when file does not exist")
  void shouldThrowFileNotFoundExceptionWhenFileDoesNotExist() {
    String nonExistentPath = tempDir.resolve("missing_key.pem").toString();

    assertThatThrownBy(() -> RsaKeyLoader.loadPrivateKey(nonExistentPath))
        .isInstanceOf(FileNotFoundException.class)
        .hasMessageContaining("Key file not found");

    assertThatThrownBy(() -> RsaKeyLoader.loadPublicKey(nonExistentPath))
        .isInstanceOf(FileNotFoundException.class)
        .hasMessageContaining("Key file not found");
  }

  @Test
  @DisplayName("Should throw Exception when key file contains invalid/corrupted content")
  void shouldThrowExceptionWhenKeyFileIsInvalid() throws Exception {
    Path invalidKeyPath = tempDir.resolve("corrupted.pem");
    Files.writeString(invalidKeyPath, "-----BEGIN PRIVATE KEY-----\nNOT_VALID_BASE_64!!!\n-----END PRIVATE KEY-----");

    assertThatThrownBy(() -> RsaKeyLoader.loadPrivateKey(invalidKeyPath.toString()))
        .isInstanceOf(Exception.class);

    assertThatThrownBy(() -> RsaKeyLoader.loadPublicKey(invalidKeyPath.toString()))
        .isInstanceOf(Exception.class);
  }
}
