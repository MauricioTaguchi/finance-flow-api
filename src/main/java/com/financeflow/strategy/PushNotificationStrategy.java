package com.financeflow.strategy;

import com.financeflow.entity.Transaction;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class PushNotificationStrategy implements NotificationStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(PushNotificationStrategy.class);

    @Override
    public void send(Transaction transaction) {
        LOGGER.info("[PUSH] Transação cadastrada com sucesso: {}", transaction.getDescription());
    }

    @Override
    public String getType() {
        return "PUSH";
    }
}
