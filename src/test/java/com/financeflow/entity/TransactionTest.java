package com.financeflow.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    @Test
    void normalizesFinancialData() {
        Transaction transaction = new Transaction(" Mercado ", new BigDecimal("10.129"), " alimentação ", null);

        assertThat(transaction.getDescription()).isEqualTo("Mercado");
        assertThat(transaction.getValue()).isEqualByComparingTo("10.13");
        assertThat(transaction.getCategory()).isEqualTo("ALIMENTAÇÃO");
        assertThat(transaction.getCreatedAt()).isNotNull();
    }

    @Test
    void enforcesDomainInvariants() {
        assertThatThrownBy(() -> new Transaction("", BigDecimal.TEN, "X", null))
                .hasMessageContaining("descrição");
        assertThatThrownBy(() -> new Transaction("X", BigDecimal.ZERO, "X", null))
                .hasMessageContaining("maior que zero");
        assertThatThrownBy(() -> new Transaction("X", BigDecimal.TEN, "", null))
                .hasMessageContaining("categoria");
    }
}
