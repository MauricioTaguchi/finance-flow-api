package com.financeflow.security;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class SchemaMigrationTest {
    @Test void freshDatabaseAndManagedLegacyMigrationPreserveQuarantine() throws Exception {
        String url = "jdbc:h2:mem:migration-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").target("1").load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) {
            sql.execute("INSERT INTO transactions (description,transaction_value,category,created_at) VALUES ('legacy',10,'TEST',CURRENT_TIMESTAMP)");
        }
        Flyway.configure().dataSource(url, "sa", "").load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) {
            var result = sql.executeQuery("SELECT COUNT(*) FROM transactions WHERE owner_id IS NULL AND description = 'legacy'");
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(1);
            assertThatThrownBy(() -> sql.execute("INSERT INTO transactions (description,transaction_value,category,created_at,owner_id) VALUES ('bad-owner',1,'TEST',CURRENT_TIMESTAMP,RANDOM_UUID())"))
                .isInstanceOf(java.sql.SQLException.class);
        }
    }
    @Test void untrackedLegacySchemaDoesNotAutomaticallyBaselineOrModifyData() throws Exception {
        String url = "jdbc:h2:mem:untracked-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) {
            sql.execute("CREATE TABLE transactions (id BIGINT PRIMARY KEY, description VARCHAR(255))");
            sql.execute("INSERT INTO transactions VALUES (1, 'preserve-me')");
        }
        assertThatThrownBy(() -> Flyway.configure().dataSource(url, "sa", "").load().migrate())
            .isInstanceOf(FlywayException.class);
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) {
            var result = sql.executeQuery("SELECT description FROM transactions WHERE id = 1");
            assertThat(result.next()).isTrue();
            assertThat(result.getString(1)).isEqualTo("preserve-me");
            assertThatThrownBy(() -> sql.executeQuery("SELECT * FROM auth_principals")).isInstanceOf(java.sql.SQLException.class);
        }
    }
}
