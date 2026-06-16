package com.financeflow.strategy;

import com.financeflow.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationStrategy implements NotificationStrategy {

    @Override
    public void send(Transaction transaction) {
        System.out.println(
                "[EMAIL] Nova transação cadastrada: "
                        + transaction.getDescription()
                        + " - R$ "
                        + transaction.getValue()
        );
    }

    @Override
    public String getType() {
        return "EMAIL";
    }
}
