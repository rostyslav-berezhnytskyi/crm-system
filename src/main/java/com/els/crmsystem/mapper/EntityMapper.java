package com.els.crmsystem.mapper;

import com.els.crmsystem.dto.input.ProjectInputDto;
import com.els.crmsystem.dto.input.UserInputDto;
import com.els.crmsystem.dto.output.ProjectOutputDto;
import com.els.crmsystem.dto.output.TransactionOutputDto;
import com.els.crmsystem.dto.output.UserOutputDto;
import com.els.crmsystem.entity.Project;
import com.els.crmsystem.entity.Transaction;
import com.els.crmsystem.entity.User;
import com.els.crmsystem.enums.Role;
import org.springframework.stereotype.Component;

@Component
public class EntityMapper {

    // ==========================================
    // USER MAPPINGS
    // ==========================================

    public UserOutputDto toOutputDto(User user) {
        if (user == null) return null;
        return new UserOutputDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.isEnabled()
        );
    }

    public User toEntity(UserInputDto dto) {
        if (dto == null) return null;
        User user = new User();
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setPassword(dto.password()); // Note: Service must Hash this later!
        user.setPhoneNumber(dto.phoneNumber());
        user.setRole(Role.CLIENT); // Default
        user.setEnabled(true);     // Default
        return user;
    }

    // ==========================================
    // PROJECT MAPPINGS
    // ==========================================

    public ProjectOutputDto toOutputDto(Project project) {
        if (project == null) return null;
        return new ProjectOutputDto(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.isActive(),
                project.getCreatedDate()
        );
    }

    public Project toEntity(ProjectInputDto dto) {
        if (dto == null) return null;
        Project project = new Project();
        project.setName(dto.name());
        project.setDescription(dto.description());
        // Default active status handled in Service or Entity logic
        if (dto.active() != null) {
            project.setActive(dto.active());
        }
        return project;
    }

    // ==========================================
    // TRANSACTION MAPPINGS
    // ==========================================

    public TransactionOutputDto toOutputDto(Transaction transaction) {
        if (transaction == null) return null;
        return new TransactionOutputDto(
                transaction.getId(),
                transaction.getProject().getName(), // Resolve name here
                transaction.getType(),
                transaction.getAmount(),
                transaction.getCategory(),
                transaction.getPaymentMethod(),
                transaction.getDescription(),
                transaction.getDate(),
                transaction.getReceiptUrl(),
                transaction.getItemImageUrl()
        );
    }
}
