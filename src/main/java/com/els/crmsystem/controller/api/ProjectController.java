package com.els.crmsystem.controller.api;

import com.els.crmsystem.dto.input.ProjectInputDto;
import com.els.crmsystem.dto.output.ProjectOutputDto;
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
    public ResponseEntity<String> createProject(@Valid @RequestBody ProjectInputDto dto) {
        projectService.createProject(dto);
        return ResponseEntity.ok("Project created successfully!");
    }

    @GetMapping
    public ResponseEntity<List<ProjectOutputDto>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }
}
