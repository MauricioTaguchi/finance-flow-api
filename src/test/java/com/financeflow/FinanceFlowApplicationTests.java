package com.financeflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.financeflow.security.TokenCodec;

@SpringBootTest
class FinanceFlowApplicationTests {

    @DynamicPropertySource
    static void bootstrap(DynamicPropertyRegistry properties) {
        String token = TokenCodec.generate();
        properties.add("app.bootstrap.admin-token", () -> token);
        properties.add("spring.datasource.url", () -> "jdbc:h2:mem:context-test");
    }

    @Test
    void contextLoads() {
    }
}
