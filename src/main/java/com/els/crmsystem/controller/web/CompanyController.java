package com.els.crmsystem.controller.web;

import com.els.crmsystem.dto.input.CompanyInputDto;
import com.els.crmsystem.enums.CompanyRole;
import com.els.crmsystem.service.CompanyDocumentService;
import com.els.crmsystem.service.CompanyService;
import com.els.crmsystem.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final CompanyDocumentService documentService;
    private final TransactionService transactionService;

    // --- AUTOMATIC DROPDOWNS ---
    @ModelAttribute("roles")
    public CompanyRole[] getCompanyRoles() {
        return CompanyRole.values();
    }

    // 1. Show the list of all active companies (WITH FILTERING & PAGINATION)
    @GetMapping
    public String listCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "name") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) CompanyRole role,
            Model model) {

        org.springframework.data.domain.Page<com.els.crmsystem.dto.output.CompanyOutputDto> companyPage =
                companyService.getFilteredAndSortedCompanies(name, role, page, size, sortField, sortDir);

        model.addAttribute("companies", companyPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", companyPage.getTotalPages());

        // Keep filters in the UI
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("currentName", name);
        model.addAttribute("currentRole", role);

        return "companies/list";
    }

    // 2. Show the "Create New Company" form
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("company", new CompanyInputDto("", CompanyRole.OTHER, "", "", "", ""));
        return "companies/form";
    }

    // View a single Company profile
    @GetMapping("/{id}")
    public String viewCompany(@PathVariable Long id, Model model) {
        model.addAttribute("company", companyService.getCompanyById(id));
        model.addAttribute("documents", documentService.getDocumentsForCompany(id));
        model.addAttribute("transactions", transactionService.getTransactionsByCompanyId(id));

        return "companies/view"; // (Or whatever your path is to company-view.html)
    }

    // 3. Process the form submission
    @PostMapping
    public String createCompany(@Valid @ModelAttribute("company") CompanyInputDto dto, BindingResult result) {
        if (result.hasErrors()) {
            return "companies/form";
        }
        companyService.createCompany(dto);
        return "redirect:/companies";
    }

    // 4. Show the "Edit Company" form
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("company", companyService.getCompanyForEdit(id));
        model.addAttribute("companyId", id); // Needed to tell the form where to POST
        return "companies/form";
    }

    // 5. Process the edit submission
    @PostMapping("/edit/{id}")
    public String updateCompany(@PathVariable Long id, @Valid @ModelAttribute("company") CompanyInputDto dto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("companyId", id);
            return "companies/form";
        }
        companyService.updateCompany(id, dto);
        return "redirect:/companies";
    }

    // 6. Deactivate (Delete) a company
    @PostMapping("/delete/{id}")
    public String deleteCompany(@PathVariable Long id) {
        companyService.deactivateCompany(id);
        return "redirect:/companies";
    }

    @PostMapping("/{id}/documents")
    public String uploadDocuments(@PathVariable Long id,
                                  @RequestParam("files") java.util.List<MultipartFile> files,
                                  @RequestParam(value = "description", required = false) String description) {
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                documentService.uploadDocument(id, file, description);
            }
        }
        return "redirect:/companies/" + id;
    }

    @PostMapping("/documents/{docId}/delete")
    public String deleteDocument(@PathVariable Long docId, @RequestParam("companyId") Long companyId) {
        documentService.deleteDocument(docId);
        return "redirect:/companies/" + companyId;
    }
}
