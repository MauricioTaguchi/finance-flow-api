package com.financeflow.controller;

import com.financeflow.dto.TransactionRequest;
import com.financeflow.entity.Transaction;
import com.financeflow.facade.TransactionFacade;
import com.financeflow.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionFacade facade;
    private final TransactionService service;

    public TransactionController(
            TransactionFacade facade,
            TransactionService service
    ) {
        this.facade = facade;
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction create(@Valid @RequestBody TransactionRequest request) {
        return facade.register(request);
    }

    @GetMapping
    public List<Transaction> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Transaction findById(@PathVariable Long id) {
        return service.findById(id);
    }
}
