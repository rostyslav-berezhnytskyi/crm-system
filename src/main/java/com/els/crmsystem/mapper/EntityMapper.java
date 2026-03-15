package com.els.crmsystem.mapper;

import com.els.crmsystem.dto.input.ProjectInputDto;
import com.els.crmsystem.dto.input.TransactionInputDto;
import com.els.crmsystem.dto.input.UserInputDto;
import com.els.crmsystem.dto.output.*;
import com.els.crmsystem.entity.*;
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
                user.isEnabled(),
                user.getPhotoUrl(),
                user.getTelegramId()
        );
    }

    public User toEntity(UserInputDto dto) {
        if (dto == null) return null;
        User user = new User();
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setPassword(dto.password()); // Note: Service must Hash this later!
        user.setPhoneNumber(dto.phoneNumber());
        user.setRole(Role.GUEST); // Default
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
                project.getCreatedDate(),
                project.getFinishDate(),

                // Safely extract names and phones if the relationships exist
                project.getClient() != null ? project.getClient().getId() : null,
                project.getClient() != null ? project.getClient().getName() : null,
                project.getClient() != null ? project.getClient().getPhone() : null,

                project.getInstaller() != null ? project.getInstaller().getId() : null,
                project.getInstaller() != null ? project.getInstaller().getName() : null,
                project.getInstaller() != null ? project.getInstaller().getPhone() : null,

                project.getEquipmentDealer() != null ? project.getEquipmentDealer().getId() : null,
                project.getEquipmentDealer() != null ? project.getEquipmentDealer().getName() : null,
                project.getEquipmentDealer() != null ? project.getEquipmentDealer().getMainPhone() : null,

                // Safely extract embedded address
                project.getAddress() != null ? project.getAddress().getText() : null,
                project.getAddress() != null ? project.getAddress().getLatitude() : null,
                project.getAddress() != null ? project.getAddress().getLongitude() : null,

                // Map the new Many-to-Many additional contacts list
                project.getAdditionalContacts().stream()
                        .map(this::toOutputDto)
                        .toList()
        );
    }

    public Project toEntity(ProjectInputDto dto) {
        if (dto == null) return null;

        Project project = new Project();
        project.setName(dto.name());
        project.setDescription(dto.description());

        if (dto.active() != null) {
            project.setActive(dto.active());
        }

        // Map the Embedded Address
        if (dto.addressText() != null) {
            com.els.crmsystem.entity.Address address = new com.els.crmsystem.entity.Address();
            address.setText(dto.addressText());
            address.setLatitude(dto.latitude());
            address.setLongitude(dto.longitude());
            project.setAddress(address);
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

    public TransactionInputDto toInputDto(Transaction transaction) {
        if (transaction == null) return null;

        // We pass NULL for the files, because we can't turn a saved file back into a "MultipartFile" object.
        // The controller handles showing the "Current File" link separately.
        return new TransactionInputDto(
                transaction.getProject().getId(),   // Extract ID for the dropdown
                transaction.getType(),
                transaction.getAmount(),
                transaction.getCategory(),
                transaction.getPaymentMethod(),
                transaction.getDescription(),
                transaction.getDate(),
                null, // receiptFile (Keep empty)
                null  // itemImageFile (Keep empty)
        );
    }

    // ==========================================
    // COMPANY MAPPINGS
    // ==========================================

    public CompanyOutputDto toOutputDto(Company company) {
        if (company == null) return null;
        return new CompanyOutputDto(
                company.getId(),
                company.getName(),
                company.getWebsite(),
                company.getMainPhone(),
                company.getEmail(),
                company.getNotes(),
                company.isActive()
        );
    }

    // ==========================================
    // CONTACT MAPPINGS
    // ==========================================

    public ContactOutputDto toOutputDto(Contact contact) {
        if (contact == null) return null;
        return new ContactOutputDto(
                contact.getId(),
                contact.getCompany() != null ? contact.getCompany().getId() : null,
                contact.getCompany() != null ? contact.getCompany().getName() : null,
                contact.getName(),
                contact.getRole(),
                contact.getPhone(),
                contact.getEmail(),
                contact.getNotes(),
                contact.isActive(),
                contact.getLastContactDate()
        );
    }

    // ==========================================
    // PROJECT MEDIA MAPPINGS
    // ==========================================

    public ProjectMediaOutputDto toOutputDto(com.els.crmsystem.entity.ProjectMedia media) {
        if (media == null) return null;

        return new ProjectMediaOutputDto(
                media.getId(),
                media.getFileUrl(),
                media.getFileType(),
                media.getDescription(),
                media.getUploadedAt()
        );
    }

    // ==========================================
    // EQUIPMENT MAPPINGS
    // ==========================================
    public EquipmentOutputDto toOutputDto(Equipment equipment) {
        if (equipment == null) return null;
        return new EquipmentOutputDto(
                equipment.getId(),
                equipment.getName(),
                equipment.getSerialNumber(),
                equipment.getWarrantyMonths(),
                equipment.getNotes()
        );
    }

    // ==========================================
    // COMPANY DOCUMENT MAPPINGS
    // ==========================================
    public CompanyDocumentOutputDto toOutputDto(CompanyDocument document) {
        if (document == null) return null;

        return new CompanyDocumentOutputDto(
                document.getId(),
                document.getFileUrl(),
                document.getFileType(),
                document.getDescription(),
                document.getUploadedAt()
        );
    }
}
