package com.els.crmsystem.service;

import com.els.crmsystem.dto.input.ContactInputDto;
import com.els.crmsystem.dto.output.ContactOutputDto;
import com.els.crmsystem.entity.Company;
import com.els.crmsystem.entity.Contact;
import com.els.crmsystem.enums.ContactRole;
import com.els.crmsystem.mapper.EntityMapper;
import com.els.crmsystem.repository.CompanyRepository;
import com.els.crmsystem.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final CompanyRepository companyRepository;
    private final EntityMapper entityMapper;

    @Transactional
    public void createContact(ContactInputDto dto) {
        Contact contact = new Contact();
        contact.setName(dto.name());
        contact.setRole(dto.role());
        contact.setPhone(dto.phone());
        contact.setEmail(dto.email());
        contact.setNotes(dto.notes());
        contact.setActive(true);

        if (dto.companyId() != null) {
            Company company = companyRepository.findById(dto.companyId())
                    .orElseThrow(() -> new IllegalArgumentException("Company not found"));
            contact.setCompany(company);
        }

        contactRepository.save(contact);
    }

    public ContactOutputDto getContactById(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Контакт не знайдено"));
        return entityMapper.toOutputDto(contact);
    }

    @Transactional
    public void updateContact(Long id, ContactInputDto dto) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found"));

        contact.setName(dto.name());
        contact.setRole(dto.role());
        contact.setPhone(dto.phone());
        contact.setEmail(dto.email());
        contact.setNotes(dto.notes());

        // Handle the optional company link safely during an update
        if (dto.companyId() != null) {
            Company company = companyRepository.findById(dto.companyId())
                    .orElseThrow(() -> new IllegalArgumentException("Company not found"));
            contact.setCompany(company);
        } else {
            contact.setCompany(null); // Unlink the company if the user cleared the dropdown
        }

        contactRepository.save(contact);
    }

    public ContactInputDto getContactForEdit(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found"));
        return new ContactInputDto(
                contact.getCompany() != null ? contact.getCompany().getId() : null,
                contact.getName(), contact.getRole(),
                contact.getPhone(), contact.getEmail(), contact.getNotes()
        );
    }

    public List<ContactOutputDto> getAllActiveContacts() {
        return contactRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(entityMapper::toOutputDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deactivateContact(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found"));
        contact.setActive(false);
        contactRepository.save(contact);
    }

    // 🔍 DYNAMIC FILTERING & SORTING FOR CONTACTS
    public Page<ContactOutputDto> getFilteredAndSortedContacts(
            String name,
            ContactRole role,
            int page,
            int size,
            String sortField,
            String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Contact> spec = com.els.crmsystem.specification.ContactSpecification.filterBy(name, role, true);

        return contactRepository.findAll(spec, pageable).map(entityMapper::toOutputDto);
    }

    public List<ContactOutputDto> getAllFilteredContacts(String name, com.els.crmsystem.enums.ContactRole role) {
        org.springframework.data.jpa.domain.Specification<Contact> spec =
                com.els.crmsystem.specification.ContactSpecification.filterBy(name, role, true);
        return contactRepository.findAll(spec).stream()
                .map(entityMapper::toOutputDto)
                .collect(Collectors.toList());
    }
}
