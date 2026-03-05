package com.els.crmsystem.controller.web;

import com.els.crmsystem.dto.input.ContactInputDto;
import com.els.crmsystem.enums.ContactRole;
import com.els.crmsystem.service.CompanyService;
import com.els.crmsystem.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;
    private final CompanyService companyService;

    // Load dropdown data for both Create and Edit forms
    private void populateDropdowns(Model model) {
        model.addAttribute("roles", ContactRole.values());
        model.addAttribute("companies", companyService.getAllActiveCompanies());
    }

    // 1. Show the list of all active contacts
    @GetMapping
    public String listContacts(Model model) {
        model.addAttribute("contacts", contactService.getAllActiveContacts());
        return "contacts/list";
    }

    // 2. Show the "Create New Contact" form
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("contact", new ContactInputDto(null, "", ContactRole.OTHER, "", "", ""));
        populateDropdowns(model);
        return "contacts/form";
    }

    // View a single Contact profile
    @GetMapping("/{id}")
    public String viewContact(@PathVariable Long id, Model model) {
        model.addAttribute("contact", contactService.getContactById(id));
        return "contacts/view";
    }

    // 3. Process the form submission
    @PostMapping
    public String createContact(@Valid @ModelAttribute("contact") ContactInputDto dto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            populateDropdowns(model); // Reload dropdowns if there is an error!
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
        populateDropdowns(model);
        return "contacts/form";
    }

    // 5. Process the edit submission
    @PostMapping("/edit/{id}")
    public String updateContact(@PathVariable Long id, @Valid @ModelAttribute("contact") ContactInputDto dto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("contactId", id);
            populateDropdowns(model);
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
}