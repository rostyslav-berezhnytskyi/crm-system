package com.els.crmsystem.controller.api;

import com.els.crmsystem.dto.TransactionDto;
import com.els.crmsystem.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<String> createTransaction(@Valid @RequestBody TransactionDto dto) {
        // Hardcoded 'admin' for now because we don't have a login system yet.
        // Make sure you CREATE a user with username 'admin' first!
        String currentUsername = "admin";

        transactionService.createTransaction(dto, currentUsername);
        return ResponseEntity.ok("Transaction saved!");
    }
}
