package com.els.crmsystem.repository;

import com.els.crmsystem.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {
    // Magic method: Only fetches companies where active = true, sorted alphabetically!
    List<Company> findAllByActiveTrueOrderByNameAsc();
}
