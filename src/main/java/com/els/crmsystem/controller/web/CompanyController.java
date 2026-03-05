package com.els.crmsystem.controller.web;

import com.els.crmsystem.dto.input.CompanyInputDto;
import com.els.crmsystem.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    // 1. Show the list of all active companies
    @GetMapping
    public String listCompanies(Model model) {
        model.addAttribute("companies", companyService.getAllActiveCompanies());
        return "companies/list"; // We will build this HTML file next
    }

    // 2. Show the "Create New Company" form
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("company", new CompanyInputDto("", "", "", "", ""));
        return "companies/form";
    }

    // View a single Company profile
    @GetMapping("/{id}")
    public String viewCompany(@PathVariable Long id, Model model) {
        model.addAttribute("company", companyService.getCompanyById(id));
        return "companies/view";
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
}
