package com.els.crmsystem.service;

import com.els.crmsystem.dto.input.TransactionInputDto;
import com.els.crmsystem.dto.output.TransactionOutputDto;
import com.els.crmsystem.entity.Project;
import com.els.crmsystem.entity.Transaction;
import com.els.crmsystem.entity.User;
import com.els.crmsystem.enums.PaymentMethod;
import com.els.crmsystem.enums.TransactionCategory;
import com.els.crmsystem.enums.TransactionType;
import com.els.crmsystem.mapper.EntityMapper;
import com.els.crmsystem.repository.*;
import com.els.crmsystem.specification.TransactionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final EntityMapper mapper;

    /**
     * Creates a new transaction with optional file attachments.
     * * @param dto Data from the form (including MultipartFiles)
     * @param username The logged-in user who is creating this record
     */
    @Transactional
    public void createTransaction(TransactionInputDto dto, String username) {

        // 1. Fetch User (Who is creating this transaction)
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // 2. Fetch Project (What project its connected)
        Project project = projectRepository.findById(dto.projectId())
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + dto.projectId()));

        // 3. Handle File Uploads (Save to Disk -> Get String Path)
        // returns null if no file sent
        String receiptPath = fileStorageService.saveTransactionFile(dto.receiptFile(), project.getId());
        String itemImagePath = fileStorageService.saveTransactionFile(dto.itemImageFile(), project.getId());

        // 4. Map DTO -> Entity
        // We do this manually here because we need to inject the found User and Project objects
        Transaction transaction = new Transaction();
        transaction.setProject(project);
        transaction.setUser(user);

        transaction.setType(dto.type());
        transaction.setAmount(dto.amount());
        transaction.setCategory(dto.category());
        transaction.setPaymentMethod(dto.paymentMethod());
        transaction.setDescription(dto.description());

        // 5. Set File Paths (if any)
        transaction.setReceiptUrl(receiptPath);
        transaction.setItemImageUrl(itemImagePath);

        // 6. Handle Date (Backdating logic)
        // If the user picked a date in the form, use it.
        // If null, the Entity's @PrePersist will automatically set it to NOW().
        if (dto.date() != null) {
            transaction.setDate(dto.date());
        }

        // --- SMART SELLER PARSING ---
        transaction.setSellerCompany(null);
        transaction.setSellerContact(null);

        if (dto.sellerValue() != null && !dto.sellerValue().isBlank()) {
            if (dto.sellerValue().startsWith("COMP_")) {
                Long compId = Long.parseLong(dto.sellerValue().replace("COMP_", ""));
                companyRepository.findById(compId).ifPresent(transaction::setSellerCompany);
            } else if (dto.sellerValue().startsWith("CONT_")) {
                Long contId = Long.parseLong(dto.sellerValue().replace("CONT_", ""));
                contactRepository.findById(contId).ifPresent(transaction::setSellerContact);
            }
        }

        // 7. Save
        transactionRepository.save(transaction);
    }

    @Transactional
    public void updateTransaction(Long id, TransactionInputDto dto, String username) {
        // 1. Find the existing transaction
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));

        // Security Check: If not Admin, ensure the user OWNS this transaction
        // (You can add this check if you want strict security, or rely on the Controller)

        // 2. Update Simple Fields
        Project project = projectRepository.findById(dto.projectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        transaction.setProject(project);
        transaction.setAmount(dto.amount());
        transaction.setType(dto.type());
        transaction.setCategory(dto.category());
        transaction.setPaymentMethod(dto.paymentMethod());
        transaction.setDescription(dto.description());

        // Date logic: If user provides a date, use it. Otherwise keep the old one?
        // Usually edits imply keeping the old date unless specified.
        if (dto.date() != null) {
            transaction.setDate(dto.date());
        }

        // 3. SMART FILE HANDLING

        // --- Handle Receipt ---
        if (dto.receiptFile() != null && !dto.receiptFile().isEmpty()) {
            deleteFile(transaction.getReceiptUrl());
            String newFileName = fileStorageService.saveTransactionFile(dto.receiptFile(), project.getId());
            transaction.setReceiptUrl(newFileName);
        }

        // --- Handle Item Photo ---
        if (dto.itemImageFile() != null && !dto.itemImageFile().isEmpty()) {
            deleteFile(transaction.getItemImageUrl());
            String newFileName = fileStorageService.saveTransactionFile(dto.itemImageFile(), project.getId());
            transaction.setItemImageUrl(newFileName);
        }

        // --- SMART SELLER PARSING ---
        transaction.setSellerCompany(null);
        transaction.setSellerContact(null);

        if (dto.sellerValue() != null && !dto.sellerValue().isBlank()) {
            if (dto.sellerValue().startsWith("COMP_")) {
                Long compId = Long.parseLong(dto.sellerValue().replace("COMP_", ""));
                companyRepository.findById(compId).ifPresent(transaction::setSellerCompany);
            } else if (dto.sellerValue().startsWith("CONT_")) {
                Long contId = Long.parseLong(dto.sellerValue().replace("CONT_", ""));
                contactRepository.findById(contId).ifPresent(transaction::setSellerContact);
            }
        }

        transactionRepository.save(transaction);
    }

    // Helper to delete old files from disk
    private void deleteFile(String fileName) {
        if (fileName != null && !fileName.isBlank()) {
            try {
                Path path = Paths.get("uploads").resolve(fileName);
                Files.deleteIfExists(path);
            } catch (Exception e) {
                System.err.println("Could not delete old file: " + fileName);
                // We don't throw an exception here because it's not critical
                // if a trash file remains, we just log it.
            }
        }
    }

    // --- READ METHODS (Using Output DTOs) ---

    public List<TransactionOutputDto> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(mapper::toOutputDto)
                .collect(Collectors.toList());
    }

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction with such id was not found"));
    }

    /**
     * Useful for the "Project Details" page where you want to see
     * only expenses for ONE specific project.
     */
    public List<TransactionOutputDto> getTransactionsByProjectId(Long projectId) {
        return transactionRepository.findByProjectId(projectId).stream()
                .map(mapper::toOutputDto)
                .collect(Collectors.toList());
    }

    // --- DELETE ---

    @Transactional
    public void deleteTransaction(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new RuntimeException("Transaction not found: " + id);
        }
        // Optional: In the future, you might want to delete the actual files from disk here too.
        transactionRepository.deleteById(id);
    }

    // --- PAGINATED READ METHOD ---
    public Page<TransactionOutputDto> getTransactionsPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactionPage = transactionRepository.findAllByOrderByDateDesc(pageable);

        // Magically map the Page of Entities to a Page of DTOs!
        return transactionPage.map(mapper::toOutputDto);
    }

    @Transactional(readOnly = true)
    public List<TransactionOutputDto> getTransactionsByCompanyId(Long companyId) {
        return transactionRepository.findBySellerCompanyIdOrderByDateDesc(companyId).stream()
                .map(mapper::toOutputDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionOutputDto> getTransactionsByContactId(Long contactId) {
        return transactionRepository.findBySellerContactIdOrderByDateDesc(contactId).stream()
                .map(mapper::toOutputDto)
                .toList();
    }

    //SPECIFICATION

    // You will need to import Sort and Pageable

    public Page<TransactionOutputDto> getFilteredAndSortedTransactions(
            Long projectId,
            TransactionType type,
            TransactionCategory category,
            PaymentMethod paymentMethod,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long companyId,
            Long contactId,
            int page,
            int size,
            String sortField,
            String sortDir) {

        // 1. Setup Sorting (e.g., sortField="amount", sortDir="desc")
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortField).ascending()
                : Sort.by(sortField).descending();

        // 2. Attach sorting and pagination together
        Pageable pageable = PageRequest.of(page, size, sort);

        // 3. Build the dynamic SQL Lego blocks
        Specification<Transaction> spec = TransactionSpecification.filterBy(
                projectId, type, category, paymentMethod, startDate, endDate, companyId, contactId); // PASSED HERE

        // 4. Execute the query!
        Page<Transaction> transactionPage = transactionRepository.findAll(spec, pageable);

        // 5. Convert to DTOs
        return transactionPage.map(mapper::toOutputDto);
    }

    // 🗄️ Fetches ALL matching records (unpaginated) for the FinanceService to calculate
    public List<TransactionOutputDto> getAllFilteredTransactions(
            Long projectId, TransactionType type, TransactionCategory category,
            PaymentMethod paymentMethod, LocalDateTime startDate, LocalDateTime endDate,
            Long companyId, Long contactId) {

        Specification<Transaction> spec = TransactionSpecification.filterBy(
                projectId, type, category, paymentMethod, startDate, endDate, companyId, contactId);

        return transactionRepository.findAll(spec).stream()
                .map(mapper::toOutputDto)
                .collect(Collectors.toList());
    }
}