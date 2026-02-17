package com.els.crmsystem.repository;

import com.els.crmsystem.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Efficient SQL check: "SELECT count(*) > 0 FROM transactions WHERE project_id = ?"
    boolean existsByProjectId(Long projectId);

    List<Transaction> findByProjectId(Long projectId);
}
