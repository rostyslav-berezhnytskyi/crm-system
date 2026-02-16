package com.els.crmsystem.controller.api;

import com.els.crmsystem.dto.ProjectDto;
import com.els.crmsystem.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<String> createProject(@Valid @RequestBody ProjectDto dto) {
        projectService.createProject(dto);
        return ResponseEntity.ok("Project created successfully!");
    }

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }
}
