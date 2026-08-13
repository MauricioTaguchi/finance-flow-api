package com.financeflow.strategy;

import com.financeflow.entity.Transaction;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class SmsNotificationStrategy implements NotificationStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmsNotificationStrategy.class);

    @Override
    public void send(Transaction transaction) {
        LOGGER.info("[SMS] Nova transação: {} - R$ {}",
                transaction.getDescription(), transaction.getValue());
    }

    @Override
    public String getType() {
        return "SMS";
    }
}
