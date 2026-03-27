package com.els.crmsystem.repository;

import com.els.crmsystem.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long>, JpaSpecificationExecutor<Contact> {
    // Only fetches active contacts
    List<Contact> findAllByActiveTrueOrderByNameAsc();

    // Useful for finding all employees that belong to one specific dealer
    List<Contact> findByCompanyId(Long companyId);
}
