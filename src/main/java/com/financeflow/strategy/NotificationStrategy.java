package com.financeflow.strategy;

import com.financeflow.entity.Transaction;

public interface NotificationStrategy {

    void send(Transaction transaction);

    String getType();
}
