package com.financeflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(nullable = false)
    private String description;

    @Column(name = "transaction_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal value;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Transaction() {
    }

    public Transaction(
            String description,
            BigDecimal value,
            String category,
            LocalDateTime createdAt
    ) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("A descrição é obrigatória.");
        }
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero.");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("A categoria é obrigatória.");
        }
        this.description = description.trim();
        this.value = value.setScale(2, java.math.RoundingMode.HALF_UP);
        this.category = category.trim().toUpperCase(java.util.Locale.ROOT);
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public Long getId() {
        return id;
    }

    public Transaction(UUID ownerId, String description, BigDecimal value, String category, LocalDateTime createdAt) {
        this(description, value, category, createdAt);
        this.ownerId = java.util.Objects.requireNonNull(ownerId, "An owner is required for a new transaction.");
    }

    @JsonIgnore
    public UUID getOwnerId() { return ownerId; }

    public String getDescription() {
        return description;
    }

    public BigDecimal getValue() {
        return value;
    }

    public String getCategory() {
        return category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
