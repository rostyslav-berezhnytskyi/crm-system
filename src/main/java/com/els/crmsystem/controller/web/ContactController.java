package com.els.crmsystem.controller.web;

import com.els.crmsystem.dto.input.ContactInputDto;
import com.els.crmsystem.dto.output.CompanyOutputDto;
import com.els.crmsystem.dto.output.ContactOutputDto;
import com.els.crmsystem.enums.ContactRole;
import com.els.crmsystem.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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

import java.util.List;

@Controller
@RequestMapping("/contacts")
@RequiredArgsConstructor
@Slf4j
public class ContactController {

    private final ContactService contactService;
    private final CompanyService companyService;
    private final TransactionService transactionService;
    private final FinanceService financeService;
    private final ExcelExportService excelExportService;

    // --- AUTOMATIC DROPDOWNS ---
    @ModelAttribute("roles")
    public ContactRole[] getContactRoles() {
        return ContactRole.values();
    }

    @ModelAttribute("companies")
    public List<CompanyOutputDto> getActiveCompanies() {
        return companyService.getAllActiveCompanies();
    }

    // 1. Show the list of all active contacts (WITH FILTERING & PAGINATION)
    @GetMapping
    public String listContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "name") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ContactRole role,
            Model model) {

        Page<ContactOutputDto> contactPage = contactService.getFilteredAndSortedContacts(name, role, page, size, sortField, sortDir);

        model.addAttribute("contacts", contactPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", contactPage.getTotalPages());

        // Keep filters in the UI
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("currentName", name);
        model.addAttribute("currentRole", role);

        return "contacts/list";
    }

    // 2. Show the "Create New Contact" form
    @GetMapping("/new")
    public String showCreateForm(
            // ADD THIS PARAMETER: It grabs the ID from the URL if it exists!
            @RequestParam(value = "companyId", required = false) Long companyId,
            Model model) {

        // Pass the grabbed companyId into the DTO instead of 'null'
        model.addAttribute("contact", new ContactInputDto(
                companyId, // <--- If from main menu, this is null. If from Company view, this is the ID!
                "",
                ContactRole.OTHER,
                "",
                "",
                ""
        ));
        return "contacts/form";
    }

    // View a single Contact profile
    @GetMapping("/{id}")
    public String viewContact(@PathVariable Long id, Model model) {
        model.addAttribute("contact", contactService.getContactById(id));

        var transactions = transactionService.getTransactionsByContactId(id);
        model.addAttribute("transactions", transactionService.getTransactionsByContactId(id));

        if (!transactions.isEmpty()) {
            model.addAttribute("totalBalance", financeService.calculateTotalBalance(transactions));
            model.addAttribute("totalIncome", financeService.calculateTotalIncome(transactions));
            model.addAttribute("totalExpense", financeService.calculateTotalExpense(transactions));
            model.addAttribute("expenseBreakdown", financeService.getExpenseBreakdownByCategory(transactions));
        }

        return "contacts/view";
    }

    // 3. Process the form submission
    @PostMapping
    public String createContact(@Valid @ModelAttribute("contact") ContactInputDto dto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "contacts/form";
        }
        contactService.createContact(dto);
        return "redirect:/contacts";
    }

    // 4. Show the "Edit Contact" form
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("contact", contactService.getContactForEdit(id));
        model.addAttribute("contactId", id);
        return "contacts/form";
    }

    // 5. Process the edit submission
    @PostMapping("/edit/{id}")
    public String updateContact(@PathVariable Long id, @Valid @ModelAttribute("contact") ContactInputDto dto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("contactId", id);
            return "contacts/form";
        }
        contactService.updateContact(id, dto);
        return "redirect:/contacts";
    }

    // 6. Deactivate (Delete) a contact
    @PostMapping("/delete/{id}")
    public String deleteContact(@PathVariable Long id) {
        contactService.deactivateContact(id);
        return "redirect:/contacts";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTOR')")
    @GetMapping("/export") // Assuming the class has @RequestMapping("/contacts")
    public ResponseEntity<byte[]> exportContacts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ContactRole role) {

        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        log.warn("SECURITY AUDIT: User '{}' downloaded the CONTACTS Excel Database.", currentUser);

        List<ContactOutputDto> allFiltered = contactService.getAllFilteredContacts(name, role);
        byte[] excelData = excelExportService.exportContactsToExcel(allFiltered);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "contacts_export.xlsx");

        return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
    }
}