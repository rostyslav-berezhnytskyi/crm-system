package com.els.crmsystem.specification;

import com.els.crmsystem.entity.Company;
import com.els.crmsystem.enums.CompanyRole;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CompanySpecification {

    private static final List<CompanyRole> PIPELINE_ROLES = List.of(CompanyRole.LEAD, CompanyRole.PROSPECT);

    /**
     * Main CRM list filter – always excludes pipeline roles (LEAD, PROSPECT).
     */
    public static Specification<Company> filterBy(String name, CompanyRole role, Boolean active) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }

            if (name != null && !name.isBlank()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }

            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
            } else {
                // Always exclude pipeline roles from the main CRM table
                predicates.add(criteriaBuilder.not(root.get("role").in(PIPELINE_ROLES)));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Pipeline-only filter – returns ONLY LEAD and PROSPECT companies.
     */
    public static Specification<Company> filterByPipeline(Boolean active) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }

            predicates.add(root.get("role").in(PIPELINE_ROLES));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}