package com.els.crmsystem.repository;

import com.els.crmsystem.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    // Magic method: Only fetches companies where active = true, sorted alphabetically!
    List<Company> findAllByActiveTrueOrderByNameAsc();
}
