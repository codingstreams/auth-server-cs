package com.example.authserver.model;

import com.example.authserver.Maskable;
import com.example.authserver.MaskedField;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@MaskedField(value = {"passwordHash"})
public class AppUser implements Maskable {

  @Id
  @Column(name = "username", length = 100, nullable = false)
  private String username;

  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private boolean active = true;

  @Column(name = "account_non_blocked", nullable = false)
  @Builder.Default
  private boolean accountNonBlocked = true;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false)
  @Builder.Default
  @JdbcType(PostgreSQLEnumJdbcType.class)
  private AuthProvider provider = AuthProvider.LOCAL;

  @Column(name = "provider_id", length = 255)
  private String providerId;

  @Column(name = "avatar_seed", length = 100)
  private String avatarSeed;

  @Column(name = "full_name", length = 255)
  private String fullName;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private Set<Authority> authorities = new HashSet<>();

  @Override
  public String toMaskedString() {
    return "AppUser(" +
        "username='" + username + '\'' +
        ", passwordHash='" + (passwordHash != null ? "******" : "null") + '\'' +
        ", active=" + active +
        ", accountNonBlocked=" + accountNonBlocked +
        ", provider=" + provider +
        ", providerId='" + providerId + '\'' +
        ", avatarSeed='" + avatarSeed + '\'' +
        ", fullName='" + fullName + '\'' +
        ", authorities=" + authorities +
        ')';
  }

  @Override
  public String toString() {
    return toMaskedString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AppUser appUser = (AppUser) o;
    return Objects.equals(username, appUser.username);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username);
  }
}
