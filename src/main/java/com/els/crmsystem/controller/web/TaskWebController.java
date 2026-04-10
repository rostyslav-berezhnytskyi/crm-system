package com.els.crmsystem.controller.web;

import com.els.crmsystem.service.TaskGroupService;
import com.els.crmsystem.service.UserService;
import com.els.crmsystem.service.CompanyService;
import com.els.crmsystem.service.ContactService;
import com.els.crmsystem.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
}