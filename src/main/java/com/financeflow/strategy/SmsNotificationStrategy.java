package com.financeflow.strategy;

import com.financeflow.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class SmsNotificationStrategy implements NotificationStrategy {

    @Override
    public void send(Transaction transaction) {
        System.out.println(
                "[SMS] Nova transação: "
                        + transaction.getDescription()
                        + " - R$ "
                        + transaction.getValue()
        );
    }

    @Override
    public String getType() {
        return "SMS";
    }
}
