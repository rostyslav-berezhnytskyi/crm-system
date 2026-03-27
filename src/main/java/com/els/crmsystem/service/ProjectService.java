package com.els.crmsystem.service;

import com.els.crmsystem.dto.input.ProjectInputDto;
import com.els.crmsystem.dto.output.ProjectOutputDto;
import com.els.crmsystem.entity.Address;
import com.els.crmsystem.entity.Contact;
import com.els.crmsystem.entity.Project;
import com.els.crmsystem.mapper.EntityMapper;
import com.els.crmsystem.repository.CompanyRepository;
import com.els.crmsystem.repository.ContactRepository;
import com.els.crmsystem.repository.ProjectRepository;
import com.els.crmsystem.repository.TransactionRepository;
import com.els.crmsystem.specification.ProjectSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TransactionRepository transactionRepository;
    private final ContactRepository contactRepository;
    private final CompanyRepository companyRepository;
    private final EntityMapper mapper;

    @Transactional
    public void createProject(ProjectInputDto dto) {
        if (projectRepository.existsByName(dto.name())) {
            throw new RuntimeException("Project with this name already exists: " + dto.name());
        }

        Project project = mapper.toEntity(dto);
        project.setActive(true);
        project.setCreatedDate(LocalDateTime.now()); // AUTO-START

        attachRelationships(project, dto);

        projectRepository.save(project);
    }

    @Transactional
    public void updateProject(Long id, ProjectInputDto dto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + id));

        if (!project.getName().equals(dto.name()) && projectRepository.existsByName(dto.name())) {
            throw new RuntimeException("Project name already taken: " + dto.name());
        }

        project.setName(dto.name());
        project.setDescription(dto.description());

        // SMART DATE LOGIC: Check if status is actually changing
        if (dto.active() != null) {
            if (project.isActive() && !dto.active()) {
                // Project is being closed
                project.setFinishDate(LocalDateTime.now());
            } else if (!project.isActive() && dto.active()) {
                // Project is being reopened
                project.setFinishDate(null);
            }
            project.setActive(dto.active());
        }

        // Update Address
        if (project.getAddress() == null) {
            project.setAddress(new Address());
        }
        project.getAddress().setText(dto.addressText());
        project.getAddress().setLatitude(dto.latitude());
        project.getAddress().setLongitude(dto.longitude());

        attachRelationships(project, dto);

        projectRepository.save(project);
    }

    // --- Helper Method to keep code clean ---
    private void attachRelationships(Project project, ProjectInputDto dto) {
        if (dto.clientId() != null) {
            project.setClient(contactRepository.findById(dto.clientId())
                    .orElseThrow(() -> new RuntimeException("Client not found")));
        } else {
            project.setClient(null);
        }

        if (dto.installerId() != null) {
            project.setInstaller(contactRepository.findById(dto.installerId())
                    .orElseThrow(() -> new RuntimeException("Installer not found")));
        } else {
            project.setInstaller(null);
        }

        if (dto.equipmentDealerId() != null) {
            project.setEquipmentDealer(companyRepository.findById(dto.equipmentDealerId())
                    .orElseThrow(() -> new RuntimeException("Dealer not found")));
        } else {
            project.setEquipmentDealer(null);
        }
    }

    // --- READ / DELETE METHODS  ---
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

    public List<ProjectOutputDto> getAllActiveProjects() {
        return projectRepository.findByActiveTrue().stream()
                .map(mapper::toOutputDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addAdditionalContactToProject(Long projectId, Long contactId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));

        project.getAdditionalContacts().add(contact);
        projectRepository.save(project);
    }

    @Transactional
    public void removeAdditionalContactFromProject(Long projectId, Long contactId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));

        project.getAdditionalContacts().remove(contact);
        projectRepository.save(project);
    }

    @Transactional
    public void closeProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + id));
        project.setActive(false);
        project.setFinishDate(LocalDateTime.now()); // AUTO-FINISH
        projectRepository.save(project);
    }

    @Transactional
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new RuntimeException("Cannot delete. Project not found: " + id);
        }
        if (transactionRepository.existsByProjectId(id)) {
            throw new RuntimeException("Cannot delete project! It has associated transactions. Please 'Close' it instead to preserve financial history.");
        }
        projectRepository.deleteById(id);
    }

    // 🔍 DYNAMIC FILTERING & SORTING FOR PROJECTS
    public Page<ProjectOutputDto> getFilteredAndSortedProjects(
            String name,
            Boolean active,
            int page,
            int size,
            String sortField,
            String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortField).ascending()
                : Sort.by(sortField).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Project> spec = ProjectSpecification.filterBy(name, active);

        return projectRepository.findAll(spec, pageable).map(mapper::toOutputDto);
    }
}