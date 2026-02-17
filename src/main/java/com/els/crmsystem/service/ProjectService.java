package com.els.crmsystem.service;

import com.els.crmsystem.dto.input.ProjectInputDto;
import com.els.crmsystem.dto.output.ProjectOutputDto;
import com.els.crmsystem.entity.Project;
import com.els.crmsystem.mapper.EntityMapper;
import com.els.crmsystem.repository.ProjectRepository;
import com.els.crmsystem.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing Projects (Installations, Repairs, etc.).
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TransactionRepository transactionRepository;
    private final EntityMapper mapper;

    // --- CREATE ---
    @Transactional
    public void createProject(ProjectInputDto dto) {
        // 1. Validate: Name must be unique
        if (projectRepository.existsByName(dto.name())) {
            throw new RuntimeException("Project with this name already exists: " + dto.name());
        }

        // 2. Map DTO -> Entity (Using the Mapper to save code!)
        Project project = mapper.toEntity(dto);

        // 3. Force Default Rules (if mapper didn't handle them)
        // Ensure new projects are always active, even if DTO says false/null
        project.setActive(true);

        projectRepository.save(project);
    }

    // --- READ ---
    public ProjectOutputDto getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + id));
        return mapper.toOutputDto(project);
    }

    public List<ProjectOutputDto> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(mapper::toOutputDto)
                .collect(Collectors.toList());
    }

    // specific method for UI Dropdowns (only shows active jobs)
    public List<ProjectOutputDto> getAllActiveProjects() {
        return projectRepository.findByActiveTrue().stream()
                .map(mapper::toOutputDto)
                .collect(Collectors.toList());
    }

    // --- UPDATE ---
    @Transactional
    public void updateProject(Long id, ProjectInputDto dto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + id));

        // Only check for duplicate name if the name is actually changing
        if (!project.getName().equals(dto.name()) && projectRepository.existsByName(dto.name())) {
            throw new RuntimeException("Project name already taken: " + dto.name());
        }

        project.setName(dto.name());
        project.setDescription(dto.description());

        // BUG FIX: Avoid NullPointerException if DTO sends null
        if (dto.active() != null) {
            project.setActive(dto.active());
        }
    }

    // --- CLOSE (ARCHIVE) ---
    @Transactional
    public void closeProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + id));
        project.setActive(false);
    }

    // --- DELETE ---
    @Transactional
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new RuntimeException("Cannot delete. Project not found: " + id);
        }

        // SAFETY CHECK: Prevent deleting projects with financial history
        if (transactionRepository.existsByProjectId(id)) {
            throw new RuntimeException("Cannot delete project! It has associated transactions. Please 'Close' it instead to preserve financial history.");
        }

        projectRepository.deleteById(id);
    }
}