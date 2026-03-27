package com.els.crmsystem.specification;

import com.els.crmsystem.entity.Company;
import com.els.crmsystem.enums.CompanyRole;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CompanySpecification {

    public static Specification<Company> filterBy(String name, CompanyRole role, Boolean active) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by active status (we don't want to see deleted companies)
            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }

            // Search by name (case-insensitive partial match)
            if (name != null && !name.isBlank()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }

            // Filter by role (Dealer, Client, etc.)
            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
