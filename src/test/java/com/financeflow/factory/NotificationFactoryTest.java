package com.financeflow.factory;

import com.financeflow.strategy.NotificationStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationFactoryTest {

    @Test
    void selectsStrategyIgnoringCase() {
        NotificationStrategy email = mock(NotificationStrategy.class);
        when(email.getType()).thenReturn("EMAIL");
        NotificationFactory factory = new NotificationFactory(List.of(email));

        assertThat(factory.getStrategy("email")).isSameAs(email);
    }

    @Test
    void rejectsUnknownAndEmptyTypes() {
        NotificationFactory factory = new NotificationFactory(List.of());
        assertThatThrownBy(() -> factory.getStrategy("FAX")).hasMessageContaining("inválido");
        assertThatThrownBy(() -> factory.getStrategy(" ")).hasMessageContaining("obrigatório");
    }
}
