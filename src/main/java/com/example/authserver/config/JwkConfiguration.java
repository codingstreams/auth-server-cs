package com.example.authserver.config;

import com.example.authserver.util.RsaKeyLoader;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwkConfiguration {
  @Bean
  public JWKSource<SecurityContext> jwkSource(
      @Value("${security.jwt.private-key-path:certs/private.pem}") String privateKeyPath,
      @Value("${security.jwt.public-key-path:certs/public.pem}") String publicKeyPath) throws Exception {

    RSAPrivateKey privateKey = RsaKeyLoader.loadPrivateKey(privateKeyPath);
    RSAPublicKey publicKey = RsaKeyLoader.loadPublicKey(publicKeyPath);

    RSAKey rsaKey = new RSAKey.Builder(publicKey)
        .privateKey(privateKey)
        .keyID("auth-server-key-1") // Must match KId in JWT header
        .build();

    JWKSet jwkSet = new JWKSet(rsaKey);
    return new ImmutableJWKSet<>(jwkSet);
  }
}