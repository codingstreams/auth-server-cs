package com.example.authserver.service;

import com.example.authserver.util.RsaKeyLoader;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

  @Value("${security.jwt.private-key-path:certs/private.pem}")
  private String privateKeyPath = "certs/private.pem";

  @Value("${security.jwt.public-key-path:certs/public.pem}")
  private String publicKeyPath = "certs/public.pem";

  private volatile RSAPrivateKey privateKey;
  private volatile RSAPublicKey publicKey;

  public JwtService(String privateKeyPath, String publicKeyPath) {
    this.privateKeyPath = privateKeyPath;
    this.publicKeyPath = publicKeyPath;
  }

  @PostConstruct
  public void init() {
    try {
      this.privateKey = RsaKeyLoader.loadPrivateKey(privateKeyPath);
      this.publicKey = RsaKeyLoader.loadPublicKey(publicKeyPath);
      log.info("RSA keys successfully initialized for JwtService");
    } catch (Exception ex) {
      log.warn("Could not preload RSA keys on startup: {}", ex.getMessage());
    }
  }

  private RSAPrivateKey getPrivateKey() {
    if (privateKey == null) {
      synchronized (this) {
        if (privateKey == null) {
          try {
            privateKey = RsaKeyLoader.loadPrivateKey(privateKeyPath);
          } catch (Exception ex) {
            log.error("Failed to load private key from path: {}", privateKeyPath, ex);
            throw new IllegalStateException("Failed to load private key from: " + privateKeyPath, ex);
          }
        }
      }
    }
    return privateKey;
  }

  private RSAPublicKey getPublicKey() {
    if (publicKey == null) {
      synchronized (this) {
        if (publicKey == null) {
          try {
            publicKey = RsaKeyLoader.loadPublicKey(publicKeyPath);
          } catch (Exception ex) {
            log.error("Failed to load public key from path: {}", publicKeyPath, ex);
            throw new IllegalStateException("Failed to load public key from: " + publicKeyPath, ex);
          }
        }
      }
    }
    return publicKey;
  }

  // 1. Token Creation (Signed using Private Key)
  public String generateToken(String userId, String email, Set<String> roles, long expirationMinutes) {
    log.debug("Generating JWT token for userId: {}", userId);
    Instant now = Instant.now();
    Instant expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES);

    return Jwts.builder()
        .subject(userId)
        .claim("email", email)
        .claim("roles", roles != null ? roles : Set.of())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .signWith(getPrivateKey())
        .compact();
  }

  // 2. Token Parsing & Verification (Verified using Public Key)
  public Claims extractClaims(String token) {
    return Jwts.parser()
        .verifyWith(getPublicKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public boolean isTokenValid(String token) {
    if (token == null || token.isBlank()) {
      return false;
    }
    try {
      Claims claims = extractClaims(token);
      return claims != null && claims.getExpiration() != null && claims.getExpiration().after(new Date());
    } catch (JwtException | IllegalArgumentException ex) {
      log.warn("JWT validation failed: {}", ex.getMessage());
      return false;
    }
  }
}
