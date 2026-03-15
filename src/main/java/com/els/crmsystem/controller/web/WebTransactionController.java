package com.els.crmsystem.controller.web;

import com.els.crmsystem.dto.input.TransactionInputDto;
import com.els.crmsystem.dto.output.TransactionOutputDto;
import com.els.crmsystem.enums.PaymentMethod;
import com.els.crmsystem.enums.TransactionCategory;
import com.els.crmsystem.enums.TransactionType;
import com.els.crmsystem.mapper.EntityMapper;
import com.els.crmsystem.service.CompanyService;
import com.els.crmsystem.service.ContactService;
import com.els.crmsystem.service.ProjectService;
import com.els.crmsystem.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class WebTransactionController {

    private final TransactionService transactionService;
    private final ProjectService projectService;
    private final EntityMapper mapper;
    private final CompanyService companyService;
    private final ContactService contactService;

    // --- 1. LIST PAGE (WITH PAGINATION & SORTING) ---
    @GetMapping("/transactions")
    public String listTransactions(
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        // Ask the Service for Page # (page), with 15 items per page
        Page<TransactionOutputDto> transactionPage = transactionService.getTransactionsPage(page, 15);

        // Send the DTOs and Pagination data to the HTML
        model.addAttribute("transactions", transactionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactionPage.getTotalPages());

        return "transaction/transactions";
    }

    // --- 2. CREATE FORM ---
    @GetMapping("/transactions/new")
    public String showCreateForm(@RequestParam(value = "projectId", required = false) Long projectId, Model model) {
        // ADDED ONE EXTRA NULL HERE FOR sellerValue
        TransactionInputDto dto = new TransactionInputDto(
                projectId, null, null, null, null, null, null, null, null, null
        );

        model.addAttribute("transaction", dto);
        prepareDropdownData(model);

        return "transaction/transaction-form";
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
            return "transaction-form";
        }

        try {
            // Get the currently logged-in user's username
            String username = principal.getName();

            transactionService.createTransaction(dto, username);
            return "redirect:/transactions?success";

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            prepareDropdownData(model);
            return "transaction-form";
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

        return "transaction/transaction-form";
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
            return "transaction/transaction-form";
        }

        transactionService.updateTransaction(id, dto, principal.getName());
        return "redirect:/transactions?updated";
    }

    // --- HELPER: Loads Dropdown Data ---
    private void prepareDropdownData(Model model) {
        model.addAttribute("projects", projectService.getAllActiveProjects());

        // ADD THESE TWO LINES FOR THE OPTGROUPS:
        model.addAttribute("companies", companyService.getAllActiveCompanies());
        // FILTER: Only pass contacts who have the DEALER (or SELLER) role!
        // Make sure to match the exact Enum name you are using in your code.
        model.addAttribute("contacts", contactService.getAllActiveContacts().stream()
                .filter(c -> c.role() == com.els.crmsystem.enums.ContactRole.DEALER)
                .toList());

        model.addAttribute("types", TransactionType.values());
        model.addAttribute("categories", TransactionCategory.values());
        model.addAttribute("paymentMethods", PaymentMethod.values());
    }
}
