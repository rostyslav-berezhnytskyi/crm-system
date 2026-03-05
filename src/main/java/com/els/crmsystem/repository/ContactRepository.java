package com.els.crmsystem.repository;

import com.els.crmsystem.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    // Only fetches active contacts
    List<Contact> findAllByActiveTrueOrderByNameAsc();

    // Useful for finding all employees that belong to one specific dealer
    List<Contact> findByCompanyId(Long companyId);
}
