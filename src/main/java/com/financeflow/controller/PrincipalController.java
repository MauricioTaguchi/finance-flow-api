package com.financeflow.controller;

import com.financeflow.security.PrincipalAdministration;
import com.financeflow.security.PrincipalAdministration.IssuedCredential;
import com.financeflow.security.PrincipalAdministration.PrincipalView;
import com.financeflow.security.PrincipalRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/principals")
public class PrincipalController {
    private final PrincipalAdministration administration;
    public PrincipalController(PrincipalAdministration administration) { this.administration = administration; }
    public record CreatePrincipal(
        @NotNull @Pattern(regexp = "[a-z0-9][a-z0-9._-]{2,63}") String username,
        @NotNull PrincipalRole role) { }

    @GetMapping
    public ResponseEntity<List<PrincipalView>> list() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(administration.list());
    }

    @PostMapping
    public ResponseEntity<IssuedCredential> create(@Valid @RequestBody CreatePrincipal request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
            .body(administration.create(request.username(), request.role()));
    }

    @PostMapping("/{id}/rotate-token")
    public ResponseEntity<IssuedCredential> rotate(@PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(administration.rotate(id));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<Void> revoke(@PathVariable UUID id) {
        administration.revoke(id);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
