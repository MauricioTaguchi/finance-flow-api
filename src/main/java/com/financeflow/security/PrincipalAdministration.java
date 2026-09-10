package com.financeflow.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PrincipalAdministration {
    private final AuthPrincipalRepository repository;
    private final CurrentPrincipal current;
    private final JdbcTemplate jdbc;

    public PrincipalAdministration(AuthPrincipalRepository repository, CurrentPrincipal current, JdbcTemplate jdbc) {
        this.repository = repository; this.current = current; this.jdbc = jdbc;
    }

    public record PrincipalView(UUID id, String username, PrincipalRole role, boolean active, Instant createdAt) {
        static PrincipalView from(AuthPrincipal principal) {
            return new PrincipalView(principal.getId(), principal.getUsername(), principal.getRole(),
                principal.isActive(), principal.getCreatedAt());
        }
    }
    public record IssuedCredential(PrincipalView principal, String token) {
        @Override public String toString() { return "IssuedCredential[principal=" + principal.id() + ", token=REDACTED]"; }
    }

    private void lockAdministration() {
        jdbc.queryForObject("SELECT id FROM auth_administration_lock WHERE id = 1 FOR UPDATE", Integer.class);
    }

    private AuthPrincipal requireAdministrator() {
        AuthenticatedPrincipal actor = current.require();
        AuthPrincipal persisted = repository.findById(actor.id()).orElseThrow(() -> new AccessDeniedException("Access denied."));
        if (!persisted.isActive() || persisted.getRole() != PrincipalRole.ADMIN
                || !actor.tokenHash().equals(persisted.getTokenHash())) {
            throw new AccessDeniedException("Access denied.");
        }
        return persisted;
    }

    @Transactional
    public void bootstrap(String username, String token) {
        lockAdministration();
        if (repository.count() == 0) {
            if (!validUsername(username) || !TokenCodec.acceptableBootstrap(token)) {
                throw new IllegalStateException("A fresh database requires a valid bootstrap username and a strong random 32-byte base64url APP_BOOTSTRAP_ADMIN_TOKEN.");
            }
            repository.saveAndFlush(new AuthPrincipal(username, PrincipalRole.ADMIN, TokenCodec.hash(token)));
        }
        if (repository.countByRoleAndActiveTrueAndTokenHashIsNotNull(PrincipalRole.ADMIN) == 0) {
            throw new IllegalStateException("No active administrator credential exists. Restore verified access before startup.");
        }
    }

    static boolean validUsername(String username) {
        return username != null && username.matches("[a-z0-9][a-z0-9._-]{2,63}");
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<PrincipalView> list() {
        requireAdministrator();
        return repository.findAll().stream().map(PrincipalView::from).toList();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public IssuedCredential create(String username, PrincipalRole role) {
        lockAdministration();
        requireAdministrator();
        if (!validUsername(username) || role == null) throw new IllegalArgumentException("Invalid username or role.");
        if (repository.existsByUsername(username)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists.");
        String token = TokenCodec.generate();
        AuthPrincipal principal = repository.saveAndFlush(new AuthPrincipal(username, role, TokenCodec.hash(token)));
        return new IssuedCredential(PrincipalView.from(principal), token);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public IssuedCredential rotate(UUID id) {
        lockAdministration();
        requireAdministrator();
        AuthPrincipal principal = find(id);
        String token = TokenCodec.generate();
        principal.rotateToken(TokenCodec.hash(token));
        repository.saveAndFlush(principal);
        return new IssuedCredential(PrincipalView.from(principal), token);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void revoke(UUID id) {
        lockAdministration();
        AuthPrincipal actor = requireAdministrator();
        AuthPrincipal target = find(id);
        if (actor.getId().equals(id)) throw new ResponseStatusException(HttpStatus.CONFLICT, "An administrator cannot revoke their own credential.");
        if (target.getRole() == PrincipalRole.ADMIN && target.isActive() && target.getTokenHash() != null
                && repository.countByRoleAndActiveTrueAndTokenHashIsNotNull(PrincipalRole.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The last active administrator cannot be revoked.");
        }
        target.revoke();
        repository.saveAndFlush(target);
    }

    private AuthPrincipal find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Principal not found."));
    }
}
