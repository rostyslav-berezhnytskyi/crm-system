package com.els.crmsystem.repository;

import com.els.crmsystem.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Efficient SQL check: "SELECT count(*) > 0 FROM transactions WHERE project_id = ?"
    boolean existsByProjectId(Long projectId);

    List<Transaction> findByProjectId(Long projectId);

    Page<Transaction> findAllByOrderByDateDesc(Pageable pageable);

    // Find all transactions where this company is the seller, sorted newest first
    List<Transaction> findBySellerCompanyIdOrderByDateDesc(Long companyId);

    // Find all transactions where this contact is the seller, sorted newest first
    List<Transaction> findBySellerContactIdOrderByDateDesc(Long contactId);
}
