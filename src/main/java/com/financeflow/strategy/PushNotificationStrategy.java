package com.financeflow.strategy;

import com.financeflow.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class PushNotificationStrategy implements NotificationStrategy {

    @Override
    public void send(Transaction transaction) {
        System.out.println(
                "[PUSH] Transação cadastrada com sucesso: "
                        + transaction.getDescription()
        );
    }

    @Override
    public String getType() {
        return "PUSH";
    }
}
