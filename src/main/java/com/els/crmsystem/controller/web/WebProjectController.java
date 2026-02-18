package com.els.crmsystem.controller.web;

import com.els.crmsystem.dto.input.ProjectInputDto;
import com.els.crmsystem.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class WebProjectController {

    private final ProjectService projectService;

    // 1. Show List & Create Form
    @GetMapping("/projects")
    public String showProjectsPage(Model model) {
        // Returns Output DTOs for the table
        model.addAttribute("projects", projectService.getAllProjects());
        return "projects";
    }

    // 2. Handle Create (Input DTO)
    @PostMapping("/projects")
    public String createProject(@ModelAttribute ProjectInputDto projectDto, Model model) {
        try {
            projectService.createProject(projectDto);
            return "redirect:/projects?success";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            // Important: reload the list so the table isn't empty on error
            model.addAttribute("projects", projectService.getAllProjects());
            return "projects";
        }
    }

    // 3. Show Edit Form (Get by ID)
    @GetMapping("/projects/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        // Returns Output DTO to pre-fill the form
        model.addAttribute("project", projectService.getProjectById(id));
        return "project-edit";
    }

    // 4. Handle Update (Input DTO)
    @PostMapping("/projects/update/{id}")
    public String updateProject(@PathVariable Long id,
                                @ModelAttribute ProjectInputDto projectDto,
                                Model model) {
        try {
            projectService.updateProject(id, projectDto);
            return "redirect:/projects";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            // On error, we need to keep the ID so the form knows which one we are editing
            model.addAttribute("project", projectService.getProjectById(id));
            return "project-edit";
        }
    }
}