package com.example.authserver.model;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorityId implements Serializable {

  private String user;
  private String authority;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AuthorityId that = (AuthorityId) o;
    return Objects.equals(user, that.user) && Objects.equals(authority, that.authority);
  }

  @Override
  public int hashCode() {
    return Objects.hash(user, authority);
  }
}
