package com.financeflow.repository;

import com.financeflow.entity.Transaction;
import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends Repository<Transaction, Long> {
    Transaction save(Transaction transaction);
    List<Transaction> findByOwnerIdOrderByIdDesc(UUID ownerId);
    Optional<Transaction> findByIdAndOwnerId(Long id, UUID ownerId);
}
