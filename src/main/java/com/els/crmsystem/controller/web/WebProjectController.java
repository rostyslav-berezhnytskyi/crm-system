package com.els.crmsystem.controller.web;

import com.els.crmsystem.dto.input.ProjectInputDto;
import com.els.crmsystem.dto.output.ProjectOutputDto;
import com.els.crmsystem.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class WebProjectController {

    private final ProjectService projectService;

    // 1. Show the Page (List + Create Form)
    @GetMapping("/projects")
    public String showProjectsPage(Model model) {
        // We put the list of projects into the model so Thymeleaf can draw the table
        model.addAttribute("projects", projectService.getAllProjects());
        return "projects"; // Looks for projects.html
    }

    // 2. Handle "Create Project" Form
    @PostMapping("/projects")
    public String createProject(ProjectInputDto projectInputDto, Model model) {
        try {
            projectService.createProject(projectInputDto);
            return "redirect:/projects?success"; // Redirect to clear the form
        } catch (RuntimeException e) {
            // If error (duplicate name), reload the page WITH the error message
            model.addAttribute("error", e.getMessage());
            // Important: We must reload the list, otherwise the table will be empty!
            model.addAttribute("projects", projectService.getAllProjects());
            return "projects";
        }
    }

    // 3. Show the Edit Form (GET /projects/edit/1)
    @GetMapping("/projects/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        // Fetch the existing project so we can pre-fill the form
        ProjectOutputDto project = projectService.getProjectById(id);
        model.addAttribute("project", project);
        return "project-edit"; // We will create this file next
    }

    // 4. Handle the Update (POST /projects/update/1)
    @PostMapping("/projects/update/{id}")
    public String updateProject(@PathVariable Long id, ProjectInputDto projectInputDto, Model model) {
        try {
            // We pass the ID from the URL and the data from the Form
            projectService.updateProject(id, projectInputDto);
            return "redirect:/projects"; // Success? Go back to the list
        } catch (RuntimeException e) {
            // Error? Reload the Edit page and show the message
            model.addAttribute("error", e.getMessage());
            model.addAttribute("project", projectInputDto); // Keep the user's input
            return "project-edit";
        }
    }
}
