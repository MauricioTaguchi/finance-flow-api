package com.financeflow.event;

import com.financeflow.factory.NotificationFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventListener {

    private final NotificationFactory notificationFactory;

    public TransactionEventListener(NotificationFactory notificationFactory) {
        this.notificationFactory = notificationFactory;
    }

    @EventListener
    public void handle(TransactionCreatedEvent event) {
        notificationFactory
                .getStrategy(event.notificationType())
                .send(event.transaction());
    }
}
