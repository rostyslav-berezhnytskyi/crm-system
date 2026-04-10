package com.els.crmsystem.service;

import com.els.crmsystem.dto.input.TransactionInputDto;
import com.els.crmsystem.entity.Company;
import com.els.crmsystem.entity.Contact;
import com.els.crmsystem.entity.Project;
import com.els.crmsystem.entity.Transaction;
import com.els.crmsystem.entity.User;
import com.els.crmsystem.enums.*;
import com.els.crmsystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for TransactionService.
 *
 * Strategy:
 * - @SpringBootTest boots the full application context (no web layer).
 * - An H2 in-memory database is configured via src/test/resources/application.yaml,
 *   which shadows the main application.yaml on the test classpath.
 * - Flyway is disabled; Hibernate uses create-drop to build the schema from entities.
 * - ALL five repositories (Transaction, User, Project, Company, Contact) hit the real H2 DB.
 * - @MockBean is used ONLY for non-repository beans that reach external systems:
 *     AuditNotificationService  → would make live HTTP calls to Telegram
 *     FileStorageService        → would create directories and write files to disk
 * - @Transactional at class level wraps each test in a transaction that is
 *   rolled back automatically, so the DB is clean before every test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@DisplayName("TransactionService — Integration Tests")
class TransactionServiceIntegrationTest {

    // -----------------------------------------------------------------------
    // System Under Test
    // -----------------------------------------------------------------------
    @Autowired
    private TransactionService transactionService;

    // -----------------------------------------------------------------------
    // Real repositories — all interact with the actual H2 database
    // -----------------------------------------------------------------------
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UserRepository        userRepository;
    @Autowired private ProjectRepository     projectRepository;
    @Autowired private CompanyRepository     companyRepository;
    @Autowired private ContactRepository     contactRepository;

    // -----------------------------------------------------------------------
    // Non-repository beans mocked to avoid external side-effects
    // -----------------------------------------------------------------------
    @MockitoBean private AuditNotificationService auditNotificationService;
    @MockitoBean private FileStorageService       fileStorageService;

    // -----------------------------------------------------------------------
    // Common test fixtures (recreated per test, rolled back after)
    // -----------------------------------------------------------------------
    private User    testUser;
    private Project testProject;

    @BeforeEach
    void setUp() {
        // FileStorageService: return null so no real file I/O happens
        when(fileStorageService.saveFileToSubfolder(any(), anyString())).thenReturn(null);

        testUser = new User();
        testUser.setUsername("integration-test-user");
        testUser.setPassword("bcrypt-encoded-password");
        testUser.setEmail("integration@test.com");
        testUser.setRole(Role.ADMIN);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        testProject = new Project();
        testProject.setName("Integration Test Project");
        testProject.setActive(true);
        testProject = projectRepository.save(testProject);
    }

    // -----------------------------------------------------------------------
    // Convenience builder
    // -----------------------------------------------------------------------
    private TransactionInputDto dto(Long projectId,
                                    TransactionType type,
                                    double amount,
                                    TransactionCategory category,
                                    PaymentMethod method,
                                    String description,
                                    String sellerValue,
                                    LocalDateTime date) {
        return new TransactionInputDto(
                projectId, sellerValue, type,
                BigDecimal.valueOf(amount), category, method,
                description, date, null, null);
    }

    /** Shorthand for a minimal EXPENSE transaction on the testProject. */
    private TransactionInputDto simpleExpense(double amount) {
        return dto(testProject.getId(),
                TransactionType.EXPENSE, amount,
                TransactionCategory.MATERIAL, PaymentMethod.CASH,
                "Test expense", null, null);
    }

    /** Saves a transaction via the service and returns the persisted entity. */
    private Transaction persistOne(TransactionInputDto inputDto) {
        transactionService.createTransaction(inputDto, testUser.getUsername());
        return transactionRepository.findByProjectId(testProject.getId()).get(0);
    }

    // =======================================================================
    // createTransaction
    // =======================================================================
    @Nested
    @DisplayName("createTransaction")
    class CreateTransaction {

        @Test
        @DisplayName("persists all fields correctly to the database")
        void persistsAllFields() {
            LocalDateTime specificDate = LocalDateTime.of(2024, 6, 1, 12, 0);
            TransactionInputDto input = dto(
                    testProject.getId(),
                    TransactionType.INCOME, 1500.50,
                    TransactionCategory.PAYMENT, PaymentMethod.IBAN,
                    "June payment", null, specificDate);

            transactionService.createTransaction(input, testUser.getUsername());

            List<Transaction> saved = transactionRepository.findByProjectId(testProject.getId());
            assertThat(saved).hasSize(1);

            Transaction t = saved.get(0);
            assertThat(t.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500.50));
            assertThat(t.getType()).isEqualTo(TransactionType.INCOME);
            assertThat(t.getCategory()).isEqualTo(TransactionCategory.PAYMENT);
            assertThat(t.getPaymentMethod()).isEqualTo(PaymentMethod.IBAN);
            assertThat(t.getDescription()).isEqualTo("June payment");
            assertThat(t.getDate()).isEqualTo(specificDate);
            assertThat(t.getProject().getId()).isEqualTo(testProject.getId());
            assertThat(t.getUser().getId()).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("assigns id (persisted as new entity, not transient)")
        void assignsDatabaseId() {
            transactionService.createTransaction(simpleExpense(100.0), testUser.getUsername());

            Transaction saved = transactionRepository.findByProjectId(testProject.getId()).get(0);
            assertThat(saved.getId()).isNotNull().isPositive();
        }

        @Test
        @DisplayName("null date → @PrePersist sets date to approximately now")
        void nullDate_setsDateToNow() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(2);

            transactionService.createTransaction(simpleExpense(300.0), testUser.getUsername());

            Transaction saved = transactionRepository.findByProjectId(testProject.getId()).get(0);
            assertThat(saved.getDate())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(2));
        }

        @Test
        @DisplayName("explicit date → keeps the provided date unchanged")
        void explicitDate_isPreserved() {
            LocalDateTime backdated = LocalDateTime.of(2023, 3, 15, 8, 30);
            TransactionInputDto input = dto(
                    testProject.getId(),
                    TransactionType.EXPENSE, 200.0,
                    TransactionCategory.TRANSPORT, PaymentMethod.CASH,
                    "Backdated fuel cost", null, backdated);

            transactionService.createTransaction(input, testUser.getUsername());

            Transaction saved = transactionRepository.findByProjectId(testProject.getId()).get(0);
            assertThat(saved.getDate()).isEqualTo(backdated);
        }

        @Test
        @DisplayName("seller value 'COMP_<id>' → links sellerCompany, leaves sellerContact null")
        void sellerValue_company_linksCompany() {
            Company company = new Company();
            company.setName("Solar Supplies Ltd");
            company.setRole(CompanyRole.DEALER);
            company.setActive(true);
            Company saved = companyRepository.save(company);

            TransactionInputDto input = dto(
                    testProject.getId(),
                    TransactionType.EXPENSE, 500.0,
                    TransactionCategory.EQUIPMENT, PaymentMethod.IBAN,
                    "Inverter purchase", "COMP_" + saved.getId(), null);

            transactionService.createTransaction(input, testUser.getUsername());

            Transaction t = transactionRepository.findByProjectId(testProject.getId()).get(0);
            assertThat(t.getSellerCompany()).isNotNull();
            assertThat(t.getSellerCompany().getId()).isEqualTo(saved.getId());
            assertThat(t.getSellerContact()).isNull();
        }

        @Test
        @DisplayName("seller value 'CONT_<id>' → links sellerContact, leaves sellerCompany null")
        void sellerValue_contact_linksContact() {
            Contact contact = new Contact();
            contact.setName("Vitaly OLX");
            contact.setRole(ContactRole.DEALER);
            contact.setActive(true);
            Contact saved = contactRepository.save(contact);

            TransactionInputDto input = dto(
                    testProject.getId(),
                    TransactionType.EXPENSE, 80.0,
                    TransactionCategory.MATERIAL, PaymentMethod.CASH,
                    "Cable from OLX", "CONT_" + saved.getId(), null);

            transactionService.createTransaction(input, testUser.getUsername());

            Transaction t = transactionRepository.findByProjectId(testProject.getId()).get(0);
            assertThat(t.getSellerContact()).isNotNull();
            assertThat(t.getSellerContact().getId()).isEqualTo(saved.getId());
            assertThat(t.getSellerCompany()).isNull();
        }

        @Test
        @DisplayName("blank seller value → both seller fields remain null")
        void blankSellerValue_noSeller() {
            TransactionInputDto input = dto(
                    testProject.getId(),
                    TransactionType.INCOME, 1000.0,
                    TransactionCategory.PREPAYMENT, PaymentMethod.CARD_PERSONAL,
                    "Prepayment", "", null);

            transactionService.createTransaction(input, testUser.getUsername());

            Transaction t = transactionRepository.findByProjectId(testProject.getId()).get(0);
            assertThat(t.getSellerCompany()).isNull();
            assertThat(t.getSellerContact()).isNull();
        }

        @Test
        @DisplayName("null seller value → both seller fields remain null")
        void nullSellerValue_noSeller() {
            transactionService.createTransaction(simpleExpense(50.0), testUser.getUsername());

            Transaction t = transactionRepository.findByProjectId(testProject.getId()).get(0);
            assertThat(t.getSellerCompany()).isNull();
            assertThat(t.getSellerContact()).isNull();
        }

        @Test
        @DisplayName("unknown username → throws RuntimeException with 'User not found'")
        void unknownUser_throwsRuntimeException() {
            TransactionInputDto input = simpleExpense(100.0);

            assertThatThrownBy(() ->
                    transactionService.createTransaction(input, "ghost-user"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("unknown projectId → throws RuntimeException with 'Project not found'")
        void unknownProject_throwsRuntimeException() {
            TransactionInputDto input = dto(
                    999_999L,
                    TransactionType.EXPENSE, 100.0,
                    TransactionCategory.MATERIAL, PaymentMethod.CASH,
                    "No such project", null, null);

            assertThatThrownBy(() ->
                    transactionService.createTransaction(input, testUser.getUsername()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Project not found");
        }
    }

    // =======================================================================
    // updateTransaction
    // =======================================================================
    @Nested
    @DisplayName("updateTransaction")
    class UpdateTransaction {

        @Test
        @DisplayName("updates amount, type, category, paymentMethod, and description")
        void updatesSimpleFields() {
            Transaction original = persistOne(simpleExpense(400.0));
            Long id = original.getId();

            TransactionInputDto update = dto(
                    testProject.getId(),
                    TransactionType.INCOME, 999.99,
                    TransactionCategory.PAYMENT, PaymentMethod.IBAN,
                    "Updated description", null,
                    LocalDateTime.of(2025, 1, 10, 9, 0));

            transactionService.updateTransaction(id, update, testUser.getUsername());

            Transaction updated = transactionRepository.findById(id).orElseThrow();
            assertThat(updated.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(999.99));
            assertThat(updated.getType()).isEqualTo(TransactionType.INCOME);
            assertThat(updated.getCategory()).isEqualTo(TransactionCategory.PAYMENT);
            assertThat(updated.getPaymentMethod()).isEqualTo(PaymentMethod.IBAN);
            assertThat(updated.getDescription()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("explicit date in update → replaces the stored date")
        void explicitDate_replacesOldDate() {
            Transaction original = persistOne(simpleExpense(100.0));
            LocalDateTime newDate = LocalDateTime.of(2025, 5, 20, 14, 30);

            TransactionInputDto update = dto(
                    testProject.getId(),
                    TransactionType.EXPENSE, 100.0,
                    TransactionCategory.MATERIAL, PaymentMethod.CASH,
                    "Date changed", null, newDate);

            transactionService.updateTransaction(original.getId(), update, testUser.getUsername());

            Transaction updated = transactionRepository.findById(original.getId()).orElseThrow();
            assertThat(updated.getDate()).isEqualTo(newDate);
        }

        @Test
        @DisplayName("null date in update → original date is preserved")
        void nullDate_keepsOriginalDate() {
            LocalDateTime originalDate = LocalDateTime.of(2024, 11, 11, 11, 11);
            Transaction original = persistOne(dto(
                    testProject.getId(),
                    TransactionType.EXPENSE, 200.0,
                    TransactionCategory.TRANSPORT, PaymentMethod.CASH,
                    "Original", null, originalDate));

            TransactionInputDto update = dto(
                    testProject.getId(),
                    TransactionType.EXPENSE, 200.0,
                    TransactionCategory.TRANSPORT, PaymentMethod.CASH,
                    "No date in update", null, null);   // <-- null date

            transactionService.updateTransaction(original.getId(), update, testUser.getUsername());

            Transaction updated = transactionRepository.findById(original.getId()).orElseThrow();
            assertThat(updated.getDate()).isEqualTo(originalDate);
        }

        @Test
        @DisplayName("can reassign transaction to a different project")
        void changesProject() {
            Transaction original = persistOne(simpleExpense(300.0));

            Project anotherProject = new Project();
            anotherProject.setName("Another Project");
            anotherProject.setActive(true);
            anotherProject = projectRepository.save(anotherProject);

            TransactionInputDto update = dto(
                    anotherProject.getId(),
                    TransactionType.EXPENSE, 300.0,
                    TransactionCategory.MATERIAL, PaymentMethod.CASH,
                    "Moved to other project", null, null);

            transactionService.updateTransaction(original.getId(), update, testUser.getUsername());

            Transaction updated = transactionRepository.findById(original.getId()).orElseThrow();
            assertThat(updated.getProject().getId()).isEqualTo(anotherProject.getId());
        }

        @Test
        @DisplayName("seller value 'COMP_<id>' → sets sellerCompany, clears sellerContact")
        void updateSeller_toCompany() {
            Transaction original = persistOne(simpleExpense(150.0));

            Company company = new Company();
            company.setName("New Supplier");
            company.setRole(CompanyRole.DEALER);
            company.setActive(true);
            company = companyRepository.save(company);

            TransactionInputDto update = dto(
                    testProject.getId(),
                    TransactionType.EXPENSE, 150.0,
                    TransactionCategory.MATERIAL, PaymentMethod.CASH,
                    "Now has seller", "COMP_" + company.getId(), null);

            transactionService.updateTransaction(original.getId(), update, testUser.getUsername());

            Transaction updated = transactionRepository.findById(original.getId()).orElseThrow();
            assertThat(updated.getSellerCompany()).isNotNull();
            assertThat(updated.getSellerCompany().getId()).isEqualTo(company.getId());
            assertThat(updated.getSellerContact()).isNull();
        }

        @Test
        @DisplayName("null seller value in update → clears both seller fields")
        void updateSeller_toNull_clearsBoth() {
            // Create a transaction that already has a company seller
            Company company = new Company();
            company.setName("Old Seller");
            company.setRole(CompanyRole.OTHER);
            company.setActive(true);
            company = companyRepository.save(company);

            TransactionInputDto createDto = dto(
                    testProject.getId(),
                    TransactionType.EXPENSE, 250.0,
                    TransactionCategory.EQUIPMENT, PaymentMethod.CASH,
                    "Has seller", "COMP_" + company.getId(), null);
            transactionService.createTransaction(createDto, testUser.getUsername());
            Transaction original = transactionRepository.findByProjectId(testProject.getId()).get(0);

            // Update without seller value
            TransactionInputDto update = dto(
                    testProject.getId(),
                    TransactionType.EXPENSE, 250.0,
                    TransactionCategory.EQUIPMENT, PaymentMethod.CASH,
                    "Seller removed", null, null);

            transactionService.updateTransaction(original.getId(), update, testUser.getUsername());

            Transaction updated = transactionRepository.findById(original.getId()).orElseThrow();
            assertThat(updated.getSellerCompany()).isNull();
            assertThat(updated.getSellerContact()).isNull();
        }

        @Test
        @DisplayName("non-existent id → throws IllegalArgumentException with 'Transaction not found'")
        void unknownId_throwsIllegalArgumentException() {
            TransactionInputDto update = simpleExpense(100.0);

            assertThatThrownBy(() ->
                    transactionService.updateTransaction(999_999L, update, testUser.getUsername()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Transaction not found");
        }

        @Test
        @DisplayName("non-existent projectId in update dto → throws IllegalArgumentException")
        void unknownProjectInDto_throws() {
            Transaction original = persistOne(simpleExpense(100.0));

            TransactionInputDto update = dto(
                    999_999L,
                    TransactionType.EXPENSE, 100.0,
                    TransactionCategory.MATERIAL, PaymentMethod.CASH,
                    "Bad project id", null, null);

            assertThatThrownBy(() ->
                    transactionService.updateTransaction(original.getId(), update, testUser.getUsername()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Project not found");
        }
    }

    // =======================================================================
    // deleteTransaction
    // =======================================================================
    @Nested
    @DisplayName("deleteTransaction")
    class DeleteTransaction {

        @Test
        @DisplayName("removes the entity from the database so findById returns empty")
        void removesEntityFromDatabase() {
            Transaction transaction = persistOne(simpleExpense(600.0));
            Long id = transaction.getId();
            assertThat(transactionRepository.findById(id)).isPresent();

            transactionService.deleteTransaction(id);

            assertThat(transactionRepository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("deletes only the targeted transaction, leaving others intact")
        void deletesOnlyTargetTransaction() {
            // Persist two transactions
            transactionService.createTransaction(simpleExpense(100.0), testUser.getUsername());
            transactionService.createTransaction(simpleExpense(200.0), testUser.getUsername());

            List<Transaction> both = transactionRepository.findByProjectId(testProject.getId());
            assertThat(both).hasSize(2);

            Long idToDelete = both.get(0).getId();
            Long idToKeep   = both.get(1).getId();

            transactionService.deleteTransaction(idToDelete);

            assertThat(transactionRepository.findById(idToDelete)).isEmpty();
            assertThat(transactionRepository.findById(idToKeep)).isPresent();
        }

        @Test
        @DisplayName("after deletion, project-level query no longer returns the transaction")
        void projectQueryReflectsDeletion() {
            Transaction transaction = persistOne(simpleExpense(750.0));
            assertThat(transactionRepository.findByProjectId(testProject.getId())).hasSize(1);

            transactionService.deleteTransaction(transaction.getId());

            assertThat(transactionRepository.findByProjectId(testProject.getId())).isEmpty();
        }

        @Test
        @DisplayName("non-existent id → throws RuntimeException with 'Transaction not found'")
        void unknownId_throwsRuntimeException() {
            assertThatThrownBy(() -> transactionService.deleteTransaction(999_999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Transaction not found");
        }

        @Test
        @DisplayName("calling delete twice on the same id throws on the second call")
        void deleteTwice_secondCallThrows() {
            Transaction transaction = persistOne(simpleExpense(50.0));
            Long id = transaction.getId();

            transactionService.deleteTransaction(id);

            assertThatThrownBy(() -> transactionService.deleteTransaction(id))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Transaction not found");
        }
    }
}