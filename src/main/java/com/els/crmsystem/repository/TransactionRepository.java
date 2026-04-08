package com.els.crmsystem.repository;

import com.els.crmsystem.entity.Transaction;
import com.els.crmsystem.enums.TransactionCategory;
import com.els.crmsystem.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    // Efficient SQL check: "SELECT count(*) > 0 FROM transactions WHERE project_id = ?"
    boolean existsByProjectId(Long projectId);

    List<Transaction> findByProjectId(Long projectId);

    Page<Transaction> findAllByOrderByDateDesc(Pageable pageable);

    // Find all transactions where this company is the seller, sorted newest first
    List<Transaction> findBySellerCompanyIdOrderByDateDesc(Long companyId);

    // Find all transactions where this contact is the seller, sorted newest first
    List<Transaction> findBySellerContactIdOrderByDateDesc(Long contactId);

    // Checks if a transaction exists on the exact same DAY
    boolean existsByProjectIdAndTypeAndCategoryAndAmountAndDateBetween(
            Long projectId,
            TransactionType type,
            TransactionCategory category,
            BigDecimal amount,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay
    );
}
