package com.financeflow.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BootstrapAdministrationTest {
    AuthPrincipalRepository repository;
    PrincipalAdministration administration;
    @BeforeEach void setup() {
        repository = mock(AuthPrincipalRepository.class);
        administration = new PrincipalAdministration(repository, mock(CurrentPrincipal.class), mock(JdbcTemplate.class));
    }
    @Test void freshDatabaseRequiresStrongExplicitBootstrap() {
        assertThatThrownBy(() -> administration.bootstrap("bootstrap-admin", "")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> administration.bootstrap("bootstrap-admin", "A".repeat(43))).isInstanceOf(IllegalStateException.class);
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void existingDatabaseNeverResetsAdministratorOrRecreatesMissingOne() {
        when(repository.count()).thenReturn(3L);
        when(repository.countByRoleAndActiveTrueAndTokenHashIsNotNull(PrincipalRole.ADMIN)).thenReturn(1L);
        administration.bootstrap("another-admin", TokenCodec.generate());
        verify(repository, never()).saveAndFlush(any());
        when(repository.countByRoleAndActiveTrueAndTokenHashIsNotNull(PrincipalRole.ADMIN)).thenReturn(0L);
        assertThatThrownBy(() -> administration.bootstrap("another-admin", TokenCodec.generate())).isInstanceOf(IllegalStateException.class);
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void tokenFormatAndHashAreSuitableForOpaqueAuthentication() {
        String token = TokenCodec.generate();
        assertThat(token).hasSize(43);
        assertThat(TokenCodec.isCanonical(token)).isTrue();
        assertThat(TokenCodec.hash(token)).hasSize(64).doesNotContain(token);
        assertThat(TokenCodec.isCanonical(token + "=")).isFalse();
    }
}
