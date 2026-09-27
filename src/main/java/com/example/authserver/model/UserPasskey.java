package com.example.authserver.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_passkeys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPasskey {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Column(name = "credential_id", nullable = false, unique = true, length = 512)
  private String credentialId; // Base64URL encoded credential ID

  @Column(name = "public_key_cose", nullable = false)
  private byte[] publicKeyCose; // COSE-encoded Public Key bytes

  @Column(name = "sign_count", nullable = false)
  @Builder.Default
  private long signCount = 0; // Counter to detect cloned authenticators

  @Column(name = "label", nullable = false)
  private String label; // e.g. "MacBook Pro Touch ID"

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();

  public String getName() {
    return label;
  }

  public String getFormattedCreatedAt() {
    if (createdAt == null) return "Recently";
    return java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")
        .withZone(java.time.ZoneId.systemDefault())
        .format(createdAt);
  }
}