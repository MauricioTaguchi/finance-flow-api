package com.financeflow.facade;

import com.financeflow.dto.TransactionRequest;
import com.financeflow.entity.Transaction;
import com.financeflow.service.TransactionService;
import org.springframework.stereotype.Component;

@Component
public class TransactionFacade {

    private final TransactionService transactionService;

    public TransactionFacade(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public Transaction register(TransactionRequest request) {
        return transactionService.create(
                request.description(),
                request.value(),
                request.category(),
                request.notificationType()
        );
    }
}
