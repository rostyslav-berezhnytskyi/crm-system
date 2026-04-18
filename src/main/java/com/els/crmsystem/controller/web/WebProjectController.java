package com.els.crmsystem.controller.web;

import com.els.crmsystem.dto.input.EquipmentInputDto;
import com.els.crmsystem.dto.input.ProjectInputDto;
import com.els.crmsystem.dto.output.ProjectMediaOutputDto;
import com.els.crmsystem.enums.PaymentMethod;
import com.els.crmsystem.enums.TransactionCategory;
import com.els.crmsystem.enums.TransactionType;
import com.els.crmsystem.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class WebProjectController {

    private final ProjectService projectService;
    private final ContactService contactService;
    private final CompanyService companyService;
    private final ProjectMediaService mediaService;
    private final EquipmentService equipmentService;
    private final TransactionService transactionService;
    private final FinanceService financeService;

    private final TaskService taskService;
    private final TaskGroupService taskGroupService;
    private final UserService userService;

    private void populateDropdowns(Model model) {

        // 1. Filtered List for the "Client" Dropdown (Only CLIENT contacts)
        model.addAttribute("clientContacts", contactService.getAllActiveContacts().stream()
                .filter(c -> c.role() == com.els.crmsystem.enums.ContactRole.CLIENT)
                .toList());

        // 2. Filtered List for the "Installer" Dropdown (Only INSTALLER contacts)
        model.addAttribute("installerContacts", contactService.getAllActiveContacts().stream()
                .filter(c -> c.role() == com.els.crmsystem.enums.ContactRole.INSTALLER)
                .toList());

        // 3. Filtered List for the "Equipment Dealer" Dropdown (Only DEALER companies)
        model.addAttribute("dealerCompanies", companyService.getAllActiveCompanies().stream()
                .filter(c -> c.role() == com.els.crmsystem.enums.CompanyRole.DEALER)
                .toList());
    }

    // View Project Profile (Command Center)
    @GetMapping("/projects/{id}")
    public String viewProject(@PathVariable Long id, Model model) {
        // Load core project details
        model.addAttribute("project", projectService.getProjectById(id));

        // Load media and process it in the controller
        List<ProjectMediaOutputDto> gallery = mediaService.getGalleryForProject(id);

// 1. Separate Photos AND Videos from Documents
        List<ProjectMediaOutputDto> photos = gallery.stream()
                .filter(media -> media.fileType() != null && (media.fileType().startsWith("image/") || media.fileType().startsWith("video/")))
                .collect(Collectors.toList());

        // 2. Group Documents by Folder Name (Exclude Images AND Videos)
        Map<String, List<ProjectMediaOutputDto>> docsByFolder = gallery.stream()
                .filter(media -> media.fileType() == null ||
                        (!media.fileType().startsWith("image/") && !media.fileType().startsWith("video/")))
                .collect(Collectors.groupingBy(
                        // If folderName is null or blank, group it under "Загальні документи"
                        media -> StringUtils.hasText(media.folderName()) ? media.folderName() : "Загальні документи"
                ));

        model.addAttribute("photos", photos);
        model.addAttribute("docsByFolder", docsByFolder);
        model.addAttribute("gallery", gallery); // Keep original gallery for total count if needed

        var transactions = transactionService.getTransactionsByProjectId(id);
        model.addAttribute("transactions", transactions);

        double balance = financeService.calculateTotalBalance(transactions);
        double totalIncome = financeService.calculateTotalIncome(transactions);
        double totalExpense = financeService.calculateTotalExpense(transactions);

        model.addAttribute("projectBalance", balance);
        model.addAttribute("projectIncome", totalIncome);
        model.addAttribute("projectExpense", totalExpense);

        model.addAttribute("expenseBreakdown", financeService.getExpenseBreakdownByCategory(transactions));

        model.addAttribute("equipments", equipmentService.getEquipmentForProject(id));

        // Pass all active contacts so we can show them in the "Add Contact" dropdown
        model.addAttribute("allContacts", contactService.getAllActiveContacts());

        // --- ADDED: Load Tasks and Modal Dictionaries ---
        model.addAttribute("tasks", taskService.getTasksForProject(id));
        model.addAttribute("taskGroups", taskGroupService.getAllGroups());
        model.addAttribute("users", userService.findAllActiveUsers());

        // Fast-loading empty lists for the heavy data (so the task modal doesn't slow down the page)
        model.addAttribute("projects", java.util.Collections.emptyList());
        model.addAttribute("companies", java.util.Collections.emptyList());
        model.addAttribute("contacts", java.util.Collections.emptyList());

        return "project/project-view";
    }

    // 1. Show the list of all projects (WITH FILTERING & PAGINATION)
    @GetMapping("/projects")
    public String showProjectsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "createdDate") String sortField,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean active,
            Model model) {

        org.springframework.data.domain.Page<com.els.crmsystem.dto.output.ProjectOutputDto> projectPage =
                projectService.getFilteredAndSortedProjects(name, active, page, size, sortField, sortDir);

        model.addAttribute("projects", projectPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", projectPage.getTotalPages());

        // Keep filters in the UI
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("currentName", name);
        model.addAttribute("currentActive", active);

        return "project/projects";
    }

    // 2. Show the dedicated Create Form
    @GetMapping("/projects/new")
    public String showCreateForm(Model model) {
        model.addAttribute("newProject", new ProjectInputDto("", "", true, null, null, null, "", null, null));
        populateDropdowns(model);
        return "project/project-create";
    }

    // 3. Handle Create (Input DTO)
    @PostMapping("/projects")
    public String createProject(@ModelAttribute ProjectInputDto projectDto, Model model) {
        try {
            projectService.createProject(projectDto);
            return "redirect:/projects?success";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            populateDropdowns(model);
            return "project/project-create"; // Stay on create page if error
        }
    }

    // 4. Show Edit Form WITH THE MEDIA GALLERY
    @GetMapping("/projects/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("project", projectService.getProjectById(id));
        model.addAttribute("gallery", mediaService.getGalleryForProject(id)); // LOAD PHOTOS!
        populateDropdowns(model);
        return "project/project-edit";
    }

    // 5. Handle Update (Input DTO)
    @PostMapping("/projects/update/{id}")
    public String updateProject(@PathVariable Long id, @ModelAttribute ProjectInputDto projectDto, Model model) {
        try {
            projectService.updateProject(id, projectDto);
            return "redirect:/projects";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("project", projectService.getProjectById(id));
            model.addAttribute("gallery", mediaService.getGalleryForProject(id));
            populateDropdowns(model);
            return "project/project-edit";
        }
    }

    // ==========================================
    // EQUIPMENT ENDPOINTS
    // ==========================================
    @PostMapping("/projects/{id}/equipment")
    public String saveOrUpdateEquipment(
            @PathVariable Long id,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam String name,
            @RequestParam(required = false) String serialNumber,
            @RequestParam(required = false) Integer warrantyMonths,
            @RequestParam(required = false) String notes) {

        if (equipmentId != null) {
            // Edit existing equipment!
            equipmentService.updateEquipment(equipmentId, name, serialNumber, warrantyMonths, notes);
        } else {
            // Create brand new equipment!
            EquipmentInputDto dto = new EquipmentInputDto(name, serialNumber, warrantyMonths, notes);
            equipmentService.addEquipmentToProject(id, dto);
        }

        return "redirect:/projects/" + id;
    }

    @PostMapping("/projects/equipment/{eqId}/delete")
    public String deleteEquipment(@PathVariable Long eqId, @RequestParam("projectId") Long projectId) {
        equipmentService.deleteEquipment(eqId);
        return "redirect:/projects/" + projectId;
    }

    // ==========================================
    // DELETE PROJECT ENDPOINT
    // ==========================================
    @PostMapping("/projects/delete/{id}")
    public String deleteProject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            projectService.deleteProject(id);
            // If successful, go back to the main list
            return "redirect:/projects?deleted=true";
        } catch (RuntimeException e) {
            // If it fails (e.g., has transactions), send the error message back to the view page
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + id;
        }
    }

    // ==========================================
    // MEDIA GALLERY ENDPOINTS
    // ==========================================

    // CHANGE REDIRECT HERE:
    @PostMapping("/projects/{id}/media")
    public String uploadMedia(@PathVariable Long id,
                              @RequestParam("files") java.util.List<MultipartFile> files, // <--- Now expects a List!
                              @RequestParam(value = "description", required = false) String description,
                              @RequestParam(value = "folderName", required = false) String folderName) { // <-- ADDED FOLDERNAME

        // Loop through every file the user selected
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                // We reuse your exact same service method for each file!
                mediaService.uploadMedia(id, file, description, folderName); // <-- PASS FOLDERNAME
            }
        }

        // Redirect back to the view page
        return "redirect:/projects/" + id;
    }

    // CHANGE REDIRECT HERE:
    @PostMapping("/projects/media/{mediaId}/delete")
    public String deleteMedia(@PathVariable Long mediaId, @RequestParam("projectId") Long projectId) {
        mediaService.deleteMedia(mediaId);
        // Redirect back to the view page!
        return "redirect:/projects/" + projectId;
    }

    // ==========================================
    // ADDITIONAL CONTACTS ENDPOINTS
    // ==========================================
    @PostMapping("/projects/{id}/contacts/add")
    public String addContactToProject(@PathVariable Long id, @RequestParam("contactId") Long contactId) {
        projectService.addAdditionalContactToProject(id, contactId);
        return "redirect:/projects/" + id;
    }

    @PostMapping("/projects/{projectId}/contacts/{contactId}/remove")
    public String removeContactFromProject(@PathVariable Long projectId, @PathVariable Long contactId) {
        projectService.removeAdditionalContactFromProject(projectId, contactId);
        return "redirect:/projects/" + projectId;
    }
}
