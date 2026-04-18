package com.els.crmsystem.specification;

import com.els.crmsystem.entity.Task;
import com.els.crmsystem.enums.TaskPriority;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TaskSpecification {

    public static Specification<Task> filterArchivedTasks(
            String searchText,
            TaskPriority priority,
            String assignee,
            Long projectId,
            Long companyId,
            Long contactId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. MUST BE COMPLETED (Because this is the Archive search)
            predicates.add(criteriaBuilder.isTrue(root.get("completed")));

            // 2. Text Search (Title OR Description)
            if (searchText != null && !searchText.trim().isEmpty()) {
                String likePattern = "%" + searchText.toLowerCase() + "%";
                Predicate titleMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likePattern);
                Predicate descMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likePattern);
                predicates.add(criteriaBuilder.or(titleMatch, descMatch));
            }

            // 3. Priority
            if (priority != null) {
                predicates.add(criteriaBuilder.equal(root.get("priority"), priority));
            }

            // 4. Assignee (Username match)
            if (assignee != null && !assignee.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("assignee").get("username"), assignee));
            }

            // 5. CRM Context Links
            if (projectId != null) {
                predicates.add(criteriaBuilder.equal(root.get("linkedProject").get("id"), projectId));
            }
            if (companyId != null) {
                predicates.add(criteriaBuilder.equal(root.get("linkedCompany").get("id"), companyId));
            }
            if (contactId != null) {
                predicates.add(criteriaBuilder.equal(root.get("linkedContact").get("id"), contactId));
            }

            // 6. Due Date Range
            if (dateFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), dateTo));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
