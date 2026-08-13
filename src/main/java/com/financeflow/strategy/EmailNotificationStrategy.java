package com.financeflow.strategy;

import com.financeflow.entity.Transaction;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class EmailNotificationStrategy implements NotificationStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotificationStrategy.class);

    @Override
    public void send(Transaction transaction) {
        LOGGER.info("[EMAIL] Nova transação cadastrada: {} - R$ {}",
                transaction.getDescription(), transaction.getValue());
    }

    @Override
    public String getType() {
        return "EMAIL";
    }
}
