package com.els.crmsystem.controller.web;

import com.els.crmsystem.dto.input.TransactionInputDto;
import com.els.crmsystem.enums.PaymentMethod;
import com.els.crmsystem.enums.TransactionCategory;
import com.els.crmsystem.enums.TransactionType;
import com.els.crmsystem.mapper.EntityMapper;
import com.els.crmsystem.service.ProjectService;
import com.els.crmsystem.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class WebTransactionController {

    private final TransactionService transactionService;
    private final ProjectService projectService;
    private final EntityMapper mapper;

    // --- 1. LIST PAGE ---
    @GetMapping("/transactions")
    public String showTransactionsPage(Model model) {
        // Send the list of transactions to the table
        model.addAttribute("transactions", transactionService.getAllTransactions());
        return "transaction/transactions"; // connects to templates/transactions.html
    }

    // --- 2. CREATE FORM ---
    @GetMapping("/transactions/new")
    public String showCreateForm(Model model) {
        // We need to send an empty DTO to bind the form data to
        model.addAttribute("transaction", new TransactionInputDto(
                null, null, null, null, null, null, null, null, null
        ));

        // Load data for Dropdowns (Projects, Enums)
        prepareDropdownData(model);

        return "transaction/transaction-create"; // connects to templates/transaction-create.html
    }

    // --- 3. HANDLE CREATE ACTION ---
    @PostMapping("/transactions")
    public String createTransaction(@Valid @ModelAttribute("transaction") TransactionInputDto dto,
                                    BindingResult bindingResult,
                                    Model model,
                                    Principal principal) {

        // If validation fails (e.g., negative amount), reload the page with errors
        if (bindingResult.hasErrors()) {
            prepareDropdownData(model); // Must reload dropdowns or the page crashes!
            return "transaction/transaction-create";
        }

        try {
            // Get the currently logged-in user's username
            String username = principal.getName();

            transactionService.createTransaction(dto, username);
            return "redirect:/transactions?success";

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            prepareDropdownData(model);
            return "transaction/transaction-create";
        }
    }

    // --- 4. DELETE ACTION ---
    @GetMapping("/transactions/delete/{id}")
    public String deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return "redirect:/transactions";
    }

    // --- 5. SHOW EDIT FORM ---
    @GetMapping("/transactions/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        // 1. Get Entity
        var t = transactionService.getTransactionById(id);

        // 2. Use Mapper (Clean!)
        TransactionInputDto formDto = mapper.toInputDto(t);

        // 3. Add to Model
        model.addAttribute("transaction", formDto);
        model.addAttribute("transactionId", id);

        // 4. Pass existing file URLs separately (for the "View Current Receipt" link)
        model.addAttribute("currentReceipt", t.getReceiptUrl());
        model.addAttribute("currentItemImage", t.getItemImageUrl());

        prepareDropdownData(model);

        return "transaction/transaction-edit";
    }

    // --- 6. HANDLE UPDATE ---
    @PostMapping("/transactions/update/{id}")
    public String updateTransaction(@PathVariable Long id,
                                    @Valid @ModelAttribute("transaction") TransactionInputDto dto,
                                    BindingResult bindingResult,
                                    Model model,
                                    Principal principal) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("transactionId", id);
            prepareDropdownData(model);
            return "transaction/transaction-edit";
        }

        transactionService.updateTransaction(id, dto, principal.getName());
        return "redirect:/transactions?updated";
    }

    // --- HELPER: Loads Dropdown Data ---
    private void prepareDropdownData(Model model) {
        // 1. Projects List (Only active ones!)
        model.addAttribute("projects", projectService.getAllActiveProjects());

        // 2. Enums for <select> options
        model.addAttribute("types", TransactionType.values());       // INCOME, EXPENSE
        model.addAttribute("categories", TransactionCategory.values()); // MATERIALS, SALARY...
        model.addAttribute("paymentMethods", PaymentMethod.values());   // CASH, CARD...
    }
}
