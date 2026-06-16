package com.financeflow.event;

import com.financeflow.entity.Transaction;

public record TransactionCreatedEvent(
        Transaction transaction,
        String notificationType
) {
}
