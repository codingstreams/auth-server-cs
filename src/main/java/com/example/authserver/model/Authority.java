package com.example.authserver.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "authorities")
@IdClass(AuthorityId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
public class Authority {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "username", nullable = false)
  private AppUser user;

  @Id
  @Column(name = "authority", length = 50, nullable = false)
  private String authority;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Authority authority1 = (Authority) o;
    return Objects.equals(user != null ? user.getUsername() : null,
        authority1.user != null ? authority1.user.getUsername() : null)
        && Objects.equals(authority, authority1.authority);
  }

  @Override
  public int hashCode() {
    return Objects.hash(user != null ? user.getUsername() : null, authority);
  }
}
