package com.els.crmsystem.controller.web;

import com.els.crmsystem.dto.input.EquipmentInputDto;
import com.els.crmsystem.dto.input.ProjectInputDto;
import com.els.crmsystem.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class WebProjectController {

    private final ProjectService projectService;
    private final ContactService contactService;
    private final CompanyService companyService;
    private final ProjectMediaService mediaService;
    private final EquipmentService equipmentService;
    private final TransactionService transactionService;

    private void populateDropdowns(Model model) {
        model.addAttribute("contacts", contactService.getAllActiveContacts());
        model.addAttribute("companies", companyService.getAllActiveCompanies());
    }


    // View Project Profile (Command Center)
    @GetMapping("/projects/{id}")
    public String viewProject(@PathVariable Long id, Model model) {
        // Load core project and gallery
        model.addAttribute("project", projectService.getProjectById(id));
        model.addAttribute("gallery", mediaService.getGalleryForProject(id));

        // CRITICAL FIX: Pass the transactions and equipment to the HTML!
        model.addAttribute("transactions", transactionService.getTransactionsByProjectId(id));
        model.addAttribute("equipments", equipmentService.getEquipmentForProject(id));

        // Pass all active contacts so we can show them in the "Add Contact" dropdown
        model.addAttribute("allContacts", contactService.getAllActiveContacts());

        return "project/project-view";
    }

    // 1. Show ONLY the List of Projects
    @GetMapping("/projects")
    public String showProjectsPage(Model model) {
        model.addAttribute("projects", projectService.getAllProjects());
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
    public String addEquipment(@PathVariable Long id, @ModelAttribute EquipmentInputDto dto) {
        equipmentService.addEquipmentToProject(id, dto);
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
                              @RequestParam(value = "description", required = false) String description) {

        // Loop through every file the user selected
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                // We reuse your exact same service method for each file!
                mediaService.uploadMedia(id, file, description);
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