package com.financeflow.service;

import com.financeflow.entity.Transaction;
import com.financeflow.event.TransactionCreatedEvent;
import com.financeflow.repository.TransactionRepository;
import com.financeflow.security.CurrentPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
    private final CurrentPrincipal current;

    public TransactionService(
            TransactionRepository repository,
            ApplicationEventPublisher eventPublisher,
            CurrentPrincipal current
    ) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.current = current;
    }

    @Transactional
    public Transaction create(
            String description,
            BigDecimal value,
            String category,
            String notificationType
    ) {
        Transaction transaction = new Transaction(
                current.require().id(),
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
        return repository.findByOwnerIdOrderByIdDesc(current.require().id());
    }

    @Transactional(readOnly = true)
    public Transaction findById(Long id) {
        return repository.findByIdAndOwnerId(id, current.require().id())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Transação não encontrada."
                ));
    }
}
