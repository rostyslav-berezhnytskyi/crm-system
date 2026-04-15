package com.els.crmsystem.controller.web;

import com.els.crmsystem.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskWebController {

    private final TaskGroupService taskGroupService;

    // We inject these so we can populate the dropdowns in the "Create Task" modal!
    private final UserService userService;
    private final CompanyService companyService;
    private final ContactService contactService;
    private final ProjectService projectService;
    private final TaskService taskService;

    @GetMapping
    public String showTaskBoard(Model model) {
        // Pass the columns (which automatically contain their sorted tasks) to the frontend
        model.addAttribute("taskGroups", taskGroupService.getAllGroups());

        // Pass data for the "Create Task" modal dropdowns
        model.addAttribute("users", userService.findAllActiveUsers());
        model.addAttribute("companies", companyService.getAllActiveCompanies());
        model.addAttribute("contacts", contactService.getAllActiveContacts());
        model.addAttribute("projects", projectService.getAllActiveProjects());

        return "tasks/board";
    }

    // Add this to your TaskWebController
    @GetMapping("/archive")
    public String showTaskArchive(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        // We need a method in TaskService that fetches ALL completed tasks with pagination
        org.springframework.data.domain.Page<com.els.crmsystem.dto.output.TaskOutputDto> archivePage =
                taskService.getAllCompletedTasks(page, size);

        model.addAttribute("tasks", archivePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", archivePage.getTotalPages());

        // Pass dictionaries for the filters
        model.addAttribute("users", userService.findAllActiveUsers());
        model.addAttribute("projects", projectService.getAllActiveProjects());
        model.addAttribute("companies", companyService.getAllFilteredCompanies(null, null));
        model.addAttribute("contacts", contactService.getAllFilteredContacts(null, null));

        return "tasks/archive";
    }
}