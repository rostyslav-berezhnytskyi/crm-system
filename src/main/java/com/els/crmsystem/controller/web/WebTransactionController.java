package com.els.crmsystem.controller.web;

import com.els.crmsystem.dto.input.TransactionInputDto;
import com.els.crmsystem.dto.output.TransactionOutputDto;
import com.els.crmsystem.enums.PaymentMethod;
import com.els.crmsystem.enums.TransactionCategory;
import com.els.crmsystem.enums.TransactionType;
import com.els.crmsystem.mapper.EntityMapper;
import com.els.crmsystem.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebTransactionController {

    private final TransactionService transactionService;
    private final ProjectService projectService;
    private final EntityMapper mapper;
    private final CompanyService companyService;
    private final ContactService contactService;
    private final FinanceService financeService;
    private final ExcelExportService excelExportService;
    private final AuditNotificationService auditService;

    // --- 1. LIST PAGE (WITH PAGINATION, FILTERING & SORTING) ---
    @GetMapping("/transactions")
    public String listTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "date") String sortField,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) TransactionCategory category,
            @RequestParam(required = false) String sellerValue,
            @RequestParam(required = false) String description,
            Model model) {

        // --- NEW: Parse the Seller Value ---
        Long companyId = null;
        Long contactId = null;
        if (sellerValue != null && !sellerValue.isBlank()) {
            if (sellerValue.startsWith("COMP_")) companyId = Long.parseLong(sellerValue.replace("COMP_", ""));
            else if (sellerValue.startsWith("CONT_")) contactId = Long.parseLong(sellerValue.replace("CONT_", ""));
        }

        // 1. Convert simple LocalDate from HTML into precise LocalDateTime for the Database
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(23, 59, 59) : null;

        // --- NEW: Grab the full unpaginated list and let FinanceService do the math! ---
        List<TransactionOutputDto> allFiltered = transactionService.getAllFilteredTransactions(
                projectId, type, category, paymentMethod, startDateTime, endDateTime, companyId, contactId, description);

        model.addAttribute("totalIncome", financeService.calculateTotalIncome(allFiltered));
        model.addAttribute("totalExpense", financeService.calculateTotalExpense(allFiltered));
        model.addAttribute("totalBalance", financeService.calculateTotalBalance(allFiltered));
        // -------------------------------------------------------------------------------

        // --- NEW: OFFICIAL FINANCES FOR ACCOUNTANT (Respects the page filters!) ---
        List<TransactionOutputDto> officialFiltered = allFiltered.stream()
                .filter(t -> t.paymentMethod() == com.els.crmsystem.enums.PaymentMethod.IBAN ||
                        t.paymentMethod() == com.els.crmsystem.enums.PaymentMethod.CARD_COMPANY)
                .toList();

        model.addAttribute("officialBalance", financeService.calculateTotalBalance(officialFiltered));
        model.addAttribute("officialIncome", financeService.calculateTotalIncome(officialFiltered));
        model.addAttribute("officialExpense", financeService.calculateTotalExpense(officialFiltered));
        // -------------------------------------------------------------------------------

        // 2. Ask the Service for the Filtered & Sorted Page
        Page<TransactionOutputDto> transactionPage = transactionService.getFilteredAndSortedTransactions(
                projectId, type, category, paymentMethod, startDateTime, endDateTime, companyId, contactId, page, size, sortField, sortDir, description);

        // 3. Send the Data and Pagination to HTML
        model.addAttribute("transactions", transactionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactionPage.getTotalPages());

        // 4. Send CURRENT filters back to HTML (so the dropdowns remember what you selected!)
        model.addAttribute("currentProjectId", projectId);
        model.addAttribute("currentType", type);
        model.addAttribute("currentStartDate", startDate);
        model.addAttribute("currentEndDate", endDate);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("currentCategory", category); // Remember category
        model.addAttribute("currentSellerValue", sellerValue); // Remember seller
        model.addAttribute("currentDescription", description);
        model.addAttribute("currentPaymentMethod", paymentMethod);

        // 5. Load the options for the dropdowns
        prepareDropdownData(model);

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
                                    @RequestParam(defaultValue = "false") boolean forceSave,
                                    Model model,
                                    Principal principal) {

        if (bindingResult.hasErrors()) {
            prepareDropdownData(model);
            return "transaction/transaction-form";
        }

        // --- NEW: THE SOFT DUPLICATE WARNING ---
        // We check the separate boolean flag!
        if (!forceSave) {
            if (transactionService.isPotentialDuplicate(dto)) {
                model.addAttribute("duplicateWarning", "Увага! Схожа транзакція (такий самий проєкт, сума та день) вже існує. Якщо це не помилка, поставте галочку нижче та натисніть зберегти ще раз.");
                prepareDropdownData(model);
                return "transaction/transaction-form";
            }
        }

        try {
            transactionService.createTransaction(dto, principal.getName());
            return "redirect:/transactions?success";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            prepareDropdownData(model);
            return "transaction/transaction-form";
        }
    }

    // --- 4. DELETE ACTION ---
    @PostMapping("/transactions/delete/{id}")
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
        model.addAttribute("types", TransactionType.values());
        model.addAttribute("paymentMethods", PaymentMethod.values());

        // --- NEW: Full lists for the Filter Bar ---
        model.addAttribute("companies", companyService.getAllActiveCompanies());
        model.addAttribute("contacts", contactService.getAllActiveContacts());
        // ----------------------------------------

        // 1. SPLIT CATEGORIES
        model.addAttribute("incomeCategories", java.util.Arrays.stream(TransactionCategory.values())
                .filter(c -> c.getType() == TransactionType.INCOME).toList());
        model.addAttribute("expenseCategories", java.util.Arrays.stream(TransactionCategory.values())
                .filter(c -> c.getType() == TransactionType.EXPENSE).toList());

        // 2. INCOME COUNTERPARTIES (Only Clients)
        model.addAttribute("incomeCompanies", companyService.getAllActiveCompanies().stream()
                .filter(c -> c.role() == com.els.crmsystem.enums.CompanyRole.CLIENT).toList());
        model.addAttribute("incomeContacts", contactService.getAllActiveContacts().stream()
                .filter(c -> c.role() == com.els.crmsystem.enums.ContactRole.CLIENT).toList());

        // 3. EXPENSE COUNTERPARTIES (Dealers, Installers, Managers)
        model.addAttribute("expenseCompanies", companyService.getAllActiveCompanies().stream()
                .filter(c -> c.role() == com.els.crmsystem.enums.CompanyRole.DEALER ||
                        c.role() == com.els.crmsystem.enums.CompanyRole.SUBCONTRACTOR).toList());
        model.addAttribute("expenseContacts", contactService.getAllActiveContacts().stream()
                .filter(c -> c.role() == com.els.crmsystem.enums.ContactRole.DEALER ||
                        c.role() == com.els.crmsystem.enums.ContactRole.INSTALLER ||
                        (c.role() == com.els.crmsystem.enums.ContactRole.MANAGER &&
                                c.companyName() != null &&
                                c.companyName().toUpperCase().contains("ELS"))).toList());
    }

    // --- EXCEL EXPORT ---
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTOR')")
    @GetMapping("/transactions/export")
    public ResponseEntity<byte[]> exportTransactions(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) TransactionCategory category,
            @RequestParam(required = false) String sellerValue,
            @RequestParam(required = false) String description) {

        auditService.notifyAndLog("Експорт БД", "Завантажено Excel-файл ТРАНЗАКЦІЙ");

        // 1. Parse Seller Value
        Long companyId = null;
        Long contactId = null;
        if (sellerValue != null && !sellerValue.isBlank()) {
            if (sellerValue.startsWith("COMP_")) companyId = Long.parseLong(sellerValue.replace("COMP_", ""));
            else if (sellerValue.startsWith("CONT_")) contactId = Long.parseLong(sellerValue.replace("CONT_", ""));
        }

        // 2. Parse Dates
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(23, 59, 59) : null;

        // 3. Get the FULL unpaginated list based on filters
        List<TransactionOutputDto> allFiltered = transactionService.getAllFilteredTransactions(
                projectId, type, category, paymentMethod, startDateTime, endDateTime, companyId, contactId, description);

        // 4. Generate Excel byte array
        byte[] excelData = excelExportService.exportTransactionsToExcel(allFiltered);

        // 5. Send file to browser
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "transactions_export.xlsx");

        return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
    }
}
