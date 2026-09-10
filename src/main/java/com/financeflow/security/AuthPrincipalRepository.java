package com.financeflow.security;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AuthPrincipalRepository extends JpaRepository<AuthPrincipal, UUID> {
    Optional<AuthPrincipal> findByTokenHashAndActiveTrue(String tokenHash);
    boolean existsByUsername(String username);
    long countByRoleAndActiveTrueAndTokenHashIsNotNull(PrincipalRole role);
}
