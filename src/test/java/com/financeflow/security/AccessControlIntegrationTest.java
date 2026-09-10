package com.financeflow.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AccessControlIntegrationTest {
    static final String ADMIN = TokenCodec.generate();
    @DynamicPropertySource
    static void settings(DynamicPropertyRegistry properties) {
        properties.add("app.bootstrap.admin-token", () -> ADMIN);
        properties.add("spring.datasource.url", () -> "jdbc:h2:mem:access-control-test");
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    record Account(UUID id, String token) { }

    Account provision(String role) throws Exception {
        String username = "test-" + UUID.randomUUID();
        String body = mvc.perform(post("/admin/principals").header("Authorization", "Bearer " + ADMIN)
                .contentType("application/json").content(json.writeValueAsString(Map.of("username", username, "role", role))))
            .andExpect(status().isCreated()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.principal.tokenHash").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        JsonNode response = json.readTree(body);
        return new Account(UUID.fromString(response.at("/principal/id").asText()), response.get("token").asText());
    }

    long create(Account account, String description) throws Exception {
        String body = mvc.perform(post("/transactions").header("Authorization", "Bearer " + account.token())
                .contentType("application/json").content(json.writeValueAsString(Map.of(
                    "description", description, "value", 12.50, "category", "TEST", "notificationType", "EMAIL"))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    @Test void anonymousAndNonBearerRequestsCannotReadOrWrite() throws Exception {
        mvc.perform(get("/transactions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/transactions/1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/transactions").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/principals")).andExpect(status().isUnauthorized());
        mvc.perform(get("/transactions").header("Authorization", "Basic YTpi")).andExpect(status().isUnauthorized());
        mvc.perform(get("/transactions").param("access_token", ADMIN)).andExpect(status().isUnauthorized());
        mvc.perform(get("/transactions").cookie(new jakarta.servlet.http.Cookie("token", ADMIN)))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/transactions").header("Authorization", "Bearer " + TokenCodec.generate()))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/transactions").header("Authorization", "Bearer " + ADMIN, "Bearer " + ADMIN))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/h2-console").header("Authorization", "Bearer " + ADMIN)).andExpect(status().isNotFound());
    }

    @Test void ownershipIsServerAssignedAndNoRoleCanBypassIt() throws Exception {
        Account a = provision("USER");
        Account b = provision("USER");
        long aId = create(a, "A-only");
        long bId = create(b, "B-only");
        mvc.perform(get("/transactions").header("Authorization", "Bearer " + a.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(aId));
        mvc.perform(get("/transactions/" + bId).header("Authorization", "Bearer " + a.token()))
            .andExpect(status().isNotFound());
        mvc.perform(get("/transactions/" + aId).header("Authorization", "Bearer " + b.token()))
            .andExpect(status().isNotFound());
        mvc.perform(get("/transactions/" + aId).header("Authorization", "Bearer " + ADMIN))
            .andExpect(status().isNotFound());
        mvc.perform(post("/transactions").header("Authorization", "Bearer " + a.token())
                .contentType("application/json").content(json.writeValueAsString(Map.of(
                    "description", "forged-owner", "value", 1, "category", "TEST", "notificationType", "EMAIL", "ownerId", b.id()))))
            .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT owner_id FROM transactions WHERE id = ?", UUID.class, aId)).isEqualTo(a.id());
    }

    @Test void ordinaryUserCannotManageAnyPrincipal() throws Exception {
        Account user = provision("USER");
        String auth = "Bearer " + user.token();
        mvc.perform(get("/admin/principals").header("Authorization", auth)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/principals").header("Authorization", auth).contentType("application/json")
            .content("{\"username\":\"forged-admin\",\"role\":\"ADMIN\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/principals/" + user.id() + "/rotate-token").header("Authorization", auth))
            .andExpect(status().isForbidden());
        mvc.perform(post("/admin/principals/" + user.id() + "/revoke").header("Authorization", auth))
            .andExpect(status().isForbidden());
    }

    @Test void rotationRevocationAndReactivationInvalidateOldCredentials() throws Exception {
        Account user = provision("USER");
        String stored = jdbc.queryForObject("SELECT token_hash FROM auth_principals WHERE id = ?", String.class, user.id());
        assertThat(stored).isEqualTo(TokenCodec.hash(user.token())).doesNotContain(user.token());
        String body = mvc.perform(post("/admin/principals/" + user.id() + "/rotate-token").header("Authorization", "Bearer " + ADMIN))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andReturn().getResponse().getContentAsString();
        String rotated = json.readTree(body).get("token").asText();
        mvc.perform(get("/transactions").header("Authorization", "Bearer " + user.token())).andExpect(status().isUnauthorized());
        mvc.perform(get("/transactions").header("Authorization", "Bearer " + rotated))
            .andExpect(status().isOk()).andExpect(header().doesNotExist("Set-Cookie"));
        mvc.perform(post("/admin/principals/" + user.id() + "/revoke").header("Authorization", "Bearer " + ADMIN))
            .andExpect(status().isNoContent());
        mvc.perform(get("/transactions").header("Authorization", "Bearer " + rotated)).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT token_hash FROM auth_principals WHERE id = ?", String.class, user.id())).isNull();
        mvc.perform(post("/admin/principals/" + user.id() + "/rotate-token").header("Authorization", "Bearer " + ADMIN))
            .andExpect(status().isOk()).andExpect(jsonPath("$.principal.active").value(true));
        mvc.perform(get("/admin/principals").header("Authorization", "Bearer " + ADMIN))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].token").doesNotExist())
            .andExpect(jsonPath("$[0].tokenHash").doesNotExist());
    }

    @Test void selfRevocationIsRejectedAndLegacyRowsRemainQuarantined() throws Exception {
        UUID adminId = jdbc.queryForObject("SELECT id FROM auth_principals WHERE username = 'bootstrap-admin'", UUID.class);
        mvc.perform(post("/admin/principals/" + adminId + "/revoke").header("Authorization", "Bearer " + ADMIN))
            .andExpect(status().isConflict());
        jdbc.update("INSERT INTO transactions (description,transaction_value,category,created_at,owner_id) VALUES ('legacy-quarantine',1,'TEST',CURRENT_TIMESTAMP,NULL)");
        Long id = jdbc.queryForObject("SELECT id FROM transactions WHERE description = 'legacy-quarantine'", Long.class);
        Account user = provision("USER");
        mvc.perform(get("/transactions/" + id).header("Authorization", "Bearer " + ADMIN)).andExpect(status().isNotFound());
        mvc.perform(get("/transactions/" + id).header("Authorization", "Bearer " + user.token())).andExpect(status().isNotFound());
        mvc.perform(get("/transactions").header("Authorization", "Bearer " + user.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test void concurrentAdministratorsCannotUseStaleAuthorityToRevokeEachOther() throws Exception {
        Account a = provision("ADMIN");
        Account b = provision("ADMIN");
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var first = executor.submit(() -> {
                start.await();
                return mvc.perform(post("/admin/principals/" + b.id() + "/revoke")
                    .header("Authorization", "Bearer " + a.token())).andReturn().getResponse().getStatus();
            });
            var second = executor.submit(() -> {
                start.await();
                return mvc.perform(post("/admin/principals/" + a.id() + "/revoke")
                    .header("Authorization", "Bearer " + b.token())).andReturn().getResponse().getStatus();
            });
            start.countDown();
            int firstStatus = first.get(10, TimeUnit.SECONDS);
            int secondStatus = second.get(10, TimeUnit.SECONDS);
            assertThat(List.of(firstStatus, secondStatus)).contains(204);
            assertThat(firstStatus == 204 && secondStatus == 204).isFalse();
            assertThat(firstStatus == 204 ? secondStatus : firstStatus).isIn(401, 403);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auth_principals WHERE id IN (?,?) AND active = TRUE", Integer.class, a.id(), b.id()))
                .isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
