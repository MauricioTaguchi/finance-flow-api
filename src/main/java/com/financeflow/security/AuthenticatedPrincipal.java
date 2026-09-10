package com.financeflow.security;

import java.util.UUID;

public record AuthenticatedPrincipal(UUID id, String username, PrincipalRole role, String tokenHash) {
    @Override public String toString() { return "AuthenticatedPrincipal[id=" + id + ", role=" + role + "]"; }
}
