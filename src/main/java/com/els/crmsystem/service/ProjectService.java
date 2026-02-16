package com.els.crmsystem.service;

import com.els.crmsystem.dto.ProjectDto;
import com.els.crmsystem.entity.Project;
import com.els.crmsystem.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Transactional
    public void createProject(ProjectDto dto) {
        // 1. Validate: Name must be unique
        if (projectRepository.existsByName(dto.name())) {
            throw new RuntimeException("Project with this name already exists: " + dto.name());
        }

        // 2. Map DTO -> Entity
        Project project = new Project();
        project.setName(dto.name());
        project.setDescription(dto.description());
        project.setActive(true); // Always start as Active
        project.setCreatedDate(LocalDateTime.now());

        projectRepository.save(project);
    }

    // --- READ ---
    public ProjectDto getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + id));
        return mapToDto(project);
    }

    public List<ProjectDto> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // specific method for UI Dropdowns (only shows active jobs)
    public List<ProjectDto> getAllActiveProjects() {
        return projectRepository.findByActiveTrue().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // --- UPDATE ---
    @Transactional
    public void updateProject(Long id, ProjectDto dto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + id));

        // Only check for duplicate name if the name is actually changing
        if (!project.getName().equals(dto.name()) && projectRepository.existsByName(dto.name())) {
            throw new RuntimeException("Project name already taken: " + dto.name());
        }

        project.setName(dto.name());
        project.setDescription(dto.description());
        project.setActive(dto.active());
    }

    // --- CLOSE (ARCHIVE) ---
    @Transactional
    public void closeProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + id));
        project.setActive(false); // Dirty checking saves this
    }

    // --- DELETE ---
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new RuntimeException("Cannot delete. Project not found: " + id);
        }
        // TODO: In the future, check if this project has Transactions.
        // If it does, we should BLOCK deletion to preserve financial history.
        projectRepository.deleteById(id);
    }

    // --- MAPPER HELPER ---
    private ProjectDto mapToDto(Project project) {
        return new ProjectDto(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.isActive(),
                project.getCreatedDate()
        );
    }
}