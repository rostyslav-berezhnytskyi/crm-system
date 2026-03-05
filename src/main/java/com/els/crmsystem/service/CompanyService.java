package com.els.crmsystem.service;

import com.els.crmsystem.dto.input.CompanyInputDto;
import com.els.crmsystem.dto.output.CompanyOutputDto;
import com.els.crmsystem.entity.Company;
import com.els.crmsystem.mapper.EntityMapper;
import com.els.crmsystem.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final EntityMapper entityMapper;

    @Transactional
    public void createCompany(CompanyInputDto dto) {
        Company company = new Company();
        company.setName(dto.name());
        company.setWebsite(dto.website());
        company.setMainPhone(dto.mainPhone());
        company.setEmail(dto.email());
        company.setNotes(dto.notes());
        company.setActive(true);

        companyRepository.save(company);
    }

    public CompanyOutputDto getCompanyById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Компанію не знайдено"));
        return entityMapper.toOutputDto(company);
    }

    @Transactional
    public void updateCompany(Long id, CompanyInputDto dto) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        company.setName(dto.name());
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
                company.getName(), company.getWebsite(),
                company.getMainPhone(), company.getEmail(), company.getNotes()
        );
    }

    @Transactional
    public void deactivateCompany(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        company.setActive(false);
        companyRepository.save(company);
    }
}
