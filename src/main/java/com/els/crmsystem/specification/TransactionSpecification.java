package com.els.crmsystem.specification;

import com.els.crmsystem.entity.Transaction;
import com.els.crmsystem.enums.PaymentMethod;
import com.els.crmsystem.enums.TransactionCategory;
import com.els.crmsystem.enums.TransactionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionSpecification {

    public static Specification<Transaction> filterBy(
            Long projectId,
            TransactionType type,
            TransactionCategory category,
            PaymentMethod paymentMethod,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long companyId,
            Long contactId,
            String description) {

        return (root, query, criteriaBuilder) -> {
            // This list holds our "Lego bricks"
            List<Predicate> predicates = new ArrayList<>();

            // 1. Did they select a Project? Snap it on.
            if (projectId != null) {
                predicates.add(criteriaBuilder.equal(root.get("project").get("id"), projectId));
            }

            // 2. Did they select Income or Expense? Snap it on.
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }

            // 3. Did they select a Category (e.g., Materials)? Snap it on.
            if (category != null) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }

            // 4. Did they select Cash/Card? Snap it on.
            if (paymentMethod != null) {
                predicates.add(criteriaBuilder.equal(root.get("paymentMethod"), paymentMethod));
            }

            // 5. Start Date filter
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("date"), startDate));
            }

            // 6. End Date filter
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("date"), endDate));
            }

            if (companyId != null)
                predicates.add(criteriaBuilder.equal(root.get("sellerCompany").get("id"), companyId));

            if (contactId != null)
                predicates.add(criteriaBuilder.equal(root.get("sellerContact").get("id"), contactId));

            // Search by description (case-insensitive partial match)
            if (description != null && !description.isBlank()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), "%" + description.toLowerCase() + "%"));
            }
            // Combine all the bricks with an "AND" statement
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
