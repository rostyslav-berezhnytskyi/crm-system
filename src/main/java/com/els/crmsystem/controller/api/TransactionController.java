package com.els.crmsystem.controller.api;

import com.els.crmsystem.dto.input.TransactionInputDto;
import com.els.crmsystem.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType; // Import this!
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Creates a transaction via API.
     * MUST use 'multipart/form-data' because of the file uploads.
     * We use @ModelAttribute to bind form fields + files to the DTO.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createTransaction(@Valid @ModelAttribute TransactionInputDto dto) {

        // Hardcoded 'admin' for now (Make sure this user exists in DB!)
        String currentUsername = "admin";

        transactionService.createTransaction(dto, currentUsername);

        return ResponseEntity.ok("Transaction saved successfully!");
    }
}