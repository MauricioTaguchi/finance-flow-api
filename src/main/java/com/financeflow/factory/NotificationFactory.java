package com.financeflow.factory;

import com.financeflow.strategy.NotificationStrategy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationFactory {

    private final List<NotificationStrategy> strategies;

    public NotificationFactory(List<NotificationStrategy> strategies) {
        this.strategies = strategies;
    }

    public NotificationStrategy getStrategy(String type) {
        return strategies.stream()
                .filter(strategy -> strategy.getType().equalsIgnoreCase(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de notificação inválido: " + type
                ));
    }
}
