package com.els.crmsystem.mapper;

import com.els.crmsystem.dto.input.ProjectInputDto;
import com.els.crmsystem.dto.input.TransactionInputDto;
import com.els.crmsystem.dto.input.UserInputDto;
import com.els.crmsystem.dto.output.*;
import com.els.crmsystem.entity.*;
import com.els.crmsystem.enums.Role;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

        // Figure out who the seller is for the UI Table
        String sellerName = null;
        String sellerUrl = null;

        if (transaction.getSellerCompany() != null) {
            sellerName = "🏢 " + transaction.getSellerCompany().getName();
            sellerUrl = "/companies/" + transaction.getSellerCompany().getId();
        } else if (transaction.getSellerContact() != null) {
            sellerName = "👤 " + transaction.getSellerContact().getName();
            sellerUrl = "/contacts/" + transaction.getSellerContact().getId();
        }

        return new TransactionOutputDto(
                transaction.getId(),
                transaction.getProject().getName(),
                sellerName,
                sellerUrl,
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

        // Reconstruct the prefixed string so the HTML Dropdown pre-selects the right option
        String sellerValue = null;
        if (transaction.getSellerCompany() != null) {
            sellerValue = "COMP_" + transaction.getSellerCompany().getId();
        } else if (transaction.getSellerContact() != null) {
            sellerValue = "CONT_" + transaction.getSellerContact().getId();
        }

        return new TransactionInputDto(
                transaction.getProject().getId(),
                sellerValue, // <-- Injected here
                transaction.getType(),
                transaction.getAmount(),
                transaction.getCategory(),
                transaction.getPaymentMethod(),
                transaction.getDescription(),
                transaction.getDate(),
                null,
                null
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
                company.getRole(),
                company.getWebsite(),
                company.getMainPhone(),
                company.getEmail(),
                company.getNotes(),
                company.isActive(),
                // Мапимо всі контакти цієї компанії:
                company.getContacts().stream()
                        .map(this::toOutputDto)
                        .toList()
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

        String fileName = null;
        if (StringUtils.hasText(media.getFileUrl())) {
            fileName = StringUtils.unqualify(media.getFileUrl().replace("\\", "/"));
        }

        return new ProjectMediaOutputDto(
                media.getId(),
                media.getFileUrl(),
                fileName,
                media.getFileType(),
                media.getFolderName(),
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

    // ==========================================
    // TASK ATTACHMENT MAPPINGS
    // ==========================================
    public TaskAttachmentOutputDto toOutputDto(TaskAttachment attachment) {
        return new TaskAttachmentOutputDto(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getFileUrl(),
                attachment.getUploadedAt()
        );
    }

    // ==========================================
    // TASK MAPPINGS
    // ==========================================
    public com.els.crmsystem.dto.output.TaskOutputDto toOutputDto(com.els.crmsystem.entity.Task task) {
        if (task == null) {
            return null;
        }

        return new com.els.crmsystem.dto.output.TaskOutputDto(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getDisplayOrder(),
                task.getCreatedAt(),
                task.getDueDate(),
                task.isCompleted(),

                task.getGroup() != null ? task.getGroup().getId() : null,
                task.getGroup() != null ? task.getGroup().getName() : null,
                task.getCreator() != null ? task.getCreator().getUsername() : "Система",
                task.getAssignee() != null ? task.getAssignee().getUsername() : null,
                task.getLinkedProject() != null ? task.getLinkedProject().getId() : null,
                task.getLinkedProject() != null ? task.getLinkedProject().getName() : null,
                task.getLinkedCompany() != null ? task.getLinkedCompany().getId() : null,
                task.getLinkedCompany() != null ? task.getLinkedCompany().getName() : null,
                task.getLinkedContact() != null ? task.getLinkedContact().getId() : null,
                task.getLinkedContact() != null ? task.getLinkedContact().getName() : null,

                // Map Attachments
                task.getAttachments() == null ? java.util.Collections.emptyList() :
                        task.getAttachments().stream().map(this::toOutputDto).toList(),

                // Map Subtasks (Recursive call!)
                task.getSubtasks() == null ? java.util.Collections.emptyList() :
                        task.getSubtasks().stream().map(this::toOutputDto).toList()
        );
    }
}
