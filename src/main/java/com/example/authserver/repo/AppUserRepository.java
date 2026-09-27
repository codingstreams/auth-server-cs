package com.example.authserver.repo;

import com.example.authserver.model.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, String> {

  @EntityGraph(attributePaths = {"authorities"})
  Optional<AppUser> findByUsername(String username);

  boolean existsByUsername(String username);
}
