package com.els.crmsystem.service;

import com.els.crmsystem.dto.input.CompanyInputDto;
import com.els.crmsystem.dto.output.CompanyOutputDto;
import com.els.crmsystem.entity.Company;
import com.els.crmsystem.mapper.EntityMapper;
import com.els.crmsystem.repository.CompanyRepository;
import com.els.crmsystem.specification.CompanySpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final EntityMapper entityMapper;
    private final AuditNotificationService auditService;

    @Transactional
    public void createCompany(CompanyInputDto dto) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Created new company: '{}' with role: {}, by user: {}", dto.name(), dto.role(), currentUser);

        Company company = new Company();
        company.setName(dto.name());
        company.setRole(dto.role());
        company.setWebsite(dto.website());
        company.setMainPhone(dto.mainPhone());
        company.setEmail(dto.email());
        company.setNotes(dto.notes());
        company.setActive(true);

        companyRepository.save(company);
    }

    @Transactional(readOnly = true)
    public CompanyOutputDto getCompanyById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Компанію не знайдено"));
        return entityMapper.toOutputDto(company);
    }

    @Transactional
    public void updateCompany(Long id, CompanyInputDto dto) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Updated company ID: '{}' with name: '{}', role: {}, by user: {}", id, dto.name(), dto.role(), currentUser);

        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        company.setName(dto.name());
        company.setRole(dto.role());
        company.setWebsite(dto.website());
        company.setMainPhone(dto.mainPhone());
        company.setEmail(dto.email());
        company.setNotes(dto.notes());

        companyRepository.save(company);
    }

    public List<CompanyOutputDto> getAllActiveCompanies() {
        return companyRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(entityMapper::toOutputDto)
                .collect(Collectors.toList());
    }

    public CompanyInputDto getCompanyForEdit(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        return new CompanyInputDto(
                company.getName(),
                company.getRole(),
                company.getWebsite(),
                company.getMainPhone(),
                company.getEmail(),
                company.getNotes()
        );
    }

    @Transactional
    public void deactivateCompany(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        company.setActive(false);

        auditService.notifyCriticalAlert("ВИДАЛЕНО КОМПАНІЮ: '%s' (Роль: %s)",
                company.getName(), company.getRole().name());

        companyRepository.save(company);
    }

    // 🔍 DYNAMIC FILTERING & SORTING FOR COMPANIES
    public Page<CompanyOutputDto> getFilteredAndSortedCompanies(
            String name,
            com.els.crmsystem.enums.CompanyRole role,
            int page,
            int size,
            String sortField,
            String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortField).ascending()
                : Sort.by(sortField).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // Always pass 'true' for active so we hide deleted companies
        Specification<Company> spec =
                CompanySpecification.filterBy(name, role, true);

        return companyRepository.findAll(spec, pageable).map(entityMapper::toOutputDto);
    }

    public List<CompanyOutputDto> getAllFilteredCompanies(String name, com.els.crmsystem.enums.CompanyRole role) {
        org.springframework.data.jpa.domain.Specification<Company> spec =
                com.els.crmsystem.specification.CompanySpecification.filterBy(name, role, true);
        return companyRepository.findAll(spec).stream()
                .map(entityMapper::toOutputDto)
                .collect(Collectors.toList());
    }
}
