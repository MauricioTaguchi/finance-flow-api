package com.financeflow.service;

import com.financeflow.entity.Transaction;
import com.financeflow.event.TransactionCreatedEvent;
import com.financeflow.repository.TransactionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public TransactionService(
            TransactionRepository repository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Transaction create(
            String description,
            BigDecimal value,
            String category,
            String notificationType
    ) {
        Transaction transaction = new Transaction(
                description,
                value,
                category,
                LocalDateTime.now()
        );

        Transaction savedTransaction = repository.save(transaction);

        eventPublisher.publishEvent(
                new TransactionCreatedEvent(
                        savedTransaction,
                        notificationType
                )
        );

        return savedTransaction;
    }

    @Transactional(readOnly = true)
    public List<Transaction> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Transaction findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transação não encontrada: " + id
                ));
    }
}
