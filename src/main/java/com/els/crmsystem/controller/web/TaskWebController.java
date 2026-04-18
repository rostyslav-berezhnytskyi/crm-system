package com.els.crmsystem.controller.web;

import com.els.crmsystem.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

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

    // --- REPLACED WITH SERVER-SIDE FILTERING ARCHITECTURE ---
    @GetMapping("/archive")
    public String showTaskArchive(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) com.els.crmsystem.enums.TaskPriority priority,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long contactId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Model model) {

        // 1. Call the new Specification-based method
        org.springframework.data.domain.Page<com.els.crmsystem.dto.output.TaskOutputDto> archivePage =
                taskService.getFilteredArchivedTasks(
                        searchText, priority, assignee, projectId, companyId, contactId, dateFrom, dateTo, page, size
                );

        // 2. Set Pagination data
        model.addAttribute("tasks", archivePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", archivePage.getTotalPages());

        // 3. PERSIST FILTERS IN THE UI: Send them back so the dropdowns remember what was selected
        model.addAttribute("currentSearch", searchText);
        model.addAttribute("currentPriority", priority);
        model.addAttribute("currentAssignee", assignee);
        model.addAttribute("currentProjectId", projectId);
        model.addAttribute("currentCompanyId", companyId);
        model.addAttribute("currentContactId", contactId);
        model.addAttribute("currentDateFrom", dateFrom);
        model.addAttribute("currentDateTo", dateTo);

        // 4. Pass dictionaries for the filter dropdowns
        model.addAttribute("users", userService.findAllActiveUsers());
        model.addAttribute("projects", projectService.getAllActiveProjects());

        // Note: Make sure these method names match what you have in your services!
        // Changed to getAllActive... to match standard naming if you have them,
        // or keep getAllFilteredCompanies(null, null) if that is what your service uses.
        model.addAttribute("companies", companyService.getAllActiveCompanies());
        model.addAttribute("contacts", contactService.getAllActiveContacts());

        return "tasks/archive";
    }
}