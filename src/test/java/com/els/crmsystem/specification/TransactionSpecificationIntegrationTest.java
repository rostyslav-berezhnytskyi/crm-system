package com.els.crmsystem.specification;

import com.els.crmsystem.entity.*;
import com.els.crmsystem.enums.*;
import com.els.crmsystem.repository.*;
import com.els.crmsystem.service.AuditNotificationService;
import com.els.crmsystem.service.FileStorageService;
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

/**
 * Integration tests for TransactionSpecification.
 *
 * Tests are run directly against the repository (transactionRepository.findAll(spec)),
 * bypassing the service layer. This isolates the Specification logic precisely.
 *
 * Seed data (6 transactions, created fresh per test via @Transactional rollback):
 *
 *   ID  | Project  | Type    | Category   | Payment       | Date       | Seller    | Description
 *   T1  | Alpha    | INCOME  | PAYMENT    | IBAN          | 2024-01-15 | —         | "Invoice payment"
 *   T2  | Alpha    | EXPENSE | MATERIAL   | CASH          | 2024-02-20 | CompanyA  | "Cable purchase"
 *   T3  | Beta     | EXPENSE | SALARY     | CARD_PERSONAL | 2024-03-10 | —         | "Worker payment"
 *   T4  | Alpha    | INCOME  | PREPAYMENT | CARD_COMPANY  | 2024-04-05 | ContactA  | "Advance from client"
 *   T5  | Beta     | EXPENSE | TRANSPORT  | CASH          | 2024-05-15 | CompanyB  | "Fuel expense"
 *   T6  | Alpha    | EXPENSE | MATERIAL   | CASH          | 2024-06-01 | CompanyA  | "Additional cable order"
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@DisplayName("TransactionSpecification — Integration Tests")
class TransactionSpecificationIntegrationTest {

    // ---------------------------------------------------------------------------
    // Repositories — all interact with the real H2 database
    // ---------------------------------------------------------------------------
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UserRepository        userRepository;
    @Autowired private ProjectRepository     projectRepository;
    @Autowired private CompanyRepository     companyRepository;
    @Autowired private ContactRepository     contactRepository;

    // Non-repository beans mocked to prevent external side-effects
    @MockitoBean private AuditNotificationService auditNotificationService;
    @MockitoBean private FileStorageService       fileStorageService;

    // ---------------------------------------------------------------------------
    // Named references to seeded entities
    // ---------------------------------------------------------------------------
    private Project  projectA, projectB;
    private Company  companyA, companyB;
    private Contact  contactA;
    private Long     t1, t2, t3, t4, t5, t6;

    @BeforeEach
    void seed() {
        User user = new User();
        user.setUsername("spec-tester");
        user.setPassword("irrelevant");
        user.setEmail("spec@test.com");
        user.setRole(Role.ADMIN);
        user.setEnabled(true);
        user = userRepository.save(user);

        projectA = savedProject("Project Alpha");
        projectB = savedProject("Project Beta");

        companyA = savedCompany("Solar Supplies Ltd",  CompanyRole.DEALER);
        companyB = savedCompany("Transport Co",        CompanyRole.OTHER);

        contactA = savedContact("John Seller", ContactRole.DEALER);

        // T1 — Alpha / INCOME / PAYMENT / IBAN / no seller
        t1 = save(user, projectA, TransactionType.INCOME, 500.0,
                TransactionCategory.PAYMENT, PaymentMethod.IBAN,
                "Invoice payment", LocalDateTime.of(2024, 1, 15, 10, 0),
                null, null);

        // T2 — Alpha / EXPENSE / MATERIAL / CASH / CompanyA
        t2 = save(user, projectA, TransactionType.EXPENSE, 300.0,
                TransactionCategory.MATERIAL, PaymentMethod.CASH,
                "Cable purchase", LocalDateTime.of(2024, 2, 20, 14, 0),
                companyA, null);

        // T3 — Beta / EXPENSE / SALARY / CARD_PERSONAL / no seller
        t3 = save(user, projectB, TransactionType.EXPENSE, 1200.0,
                TransactionCategory.SALARY, PaymentMethod.CARD_PERSONAL,
                "Worker payment", LocalDateTime.of(2024, 3, 10, 9, 0),
                null, null);

        // T4 — Alpha / INCOME / PREPAYMENT / CARD_COMPANY / ContactA
        t4 = save(user, projectA, TransactionType.INCOME, 800.0,
                TransactionCategory.PREPAYMENT, PaymentMethod.CARD_COMPANY,
                "Advance from client", LocalDateTime.of(2024, 4, 5, 16, 0),
                null, contactA);

        // T5 — Beta / EXPENSE / TRANSPORT / CASH / CompanyB
        t5 = save(user, projectB, TransactionType.EXPENSE, 150.0,
                TransactionCategory.TRANSPORT, PaymentMethod.CASH,
                "Fuel expense", LocalDateTime.of(2024, 5, 15, 8, 0),
                companyB, null);

        // T6 — Alpha / EXPENSE / MATERIAL / CASH / CompanyA
        t6 = save(user, projectA, TransactionType.EXPENSE, 450.0,
                TransactionCategory.MATERIAL, PaymentMethod.CASH,
                "Additional cable order", LocalDateTime.of(2024, 6, 1, 11, 0),
                companyA, null);
    }

    // ===========================================================================
    // No filters
    // ===========================================================================
    @Nested
    @DisplayName("No filters (all null)")
    class NoFilters {

        @Test
        @DisplayName("returns every transaction in the database")
        void allNull_returnsAll() {
            assertThat(ids(query(null, null, null, null, null, null, null, null, null)))
                    .containsExactlyInAnyOrder(t1, t2, t3, t4, t5, t6);
        }
    }

    // ===========================================================================
    // Single filters
    // ===========================================================================
    @Nested
    @DisplayName("Single filter — each dimension in isolation")
    class SingleFilter {

        @Test
        @DisplayName("projectId=Alpha → T1, T2, T4, T6")
        void byProjectA() {
            assertThat(ids(query(projectA.getId(), null, null, null, null, null, null, null, null)))
                    .containsExactlyInAnyOrder(t1, t2, t4, t6);
        }

        @Test
        @DisplayName("projectId=Beta → T3, T5")
        void byProjectB() {
            assertThat(ids(query(projectB.getId(), null, null, null, null, null, null, null, null)))
                    .containsExactlyInAnyOrder(t3, t5);
        }

        @Test
        @DisplayName("type=INCOME → T1, T4")
        void byTypeIncome() {
            assertThat(ids(query(null, TransactionType.INCOME, null, null, null, null, null, null, null)))
                    .containsExactlyInAnyOrder(t1, t4);
        }

        @Test
        @DisplayName("type=EXPENSE → T2, T3, T5, T6")
        void byTypeExpense() {
            assertThat(ids(query(null, TransactionType.EXPENSE, null, null, null, null, null, null, null)))
                    .containsExactlyInAnyOrder(t2, t3, t5, t6);
        }

        @Test
        @DisplayName("category=MATERIAL → T2, T6")
        void byCategoryMaterial() {
            assertThat(ids(query(null, null, TransactionCategory.MATERIAL, null, null, null, null, null, null)))
                    .containsExactlyInAnyOrder(t2, t6);
        }

        @Test
        @DisplayName("category=SALARY → T3 only")
        void byCategorySalary() {
            assertThat(ids(query(null, null, TransactionCategory.SALARY, null, null, null, null, null, null)))
                    .containsExactlyInAnyOrder(t3);
        }

        @Test
        @DisplayName("paymentMethod=CASH → T2, T5, T6")
        void byPaymentMethodCash() {
            assertThat(ids(query(null, null, null, PaymentMethod.CASH, null, null, null, null, null)))
                    .containsExactlyInAnyOrder(t2, t5, t6);
        }

        @Test
        @DisplayName("paymentMethod=IBAN → T1 only")
        void byPaymentMethodIban() {
            assertThat(ids(query(null, null, null, PaymentMethod.IBAN, null, null, null, null, null)))
                    .containsExactlyInAnyOrder(t1);
        }

        @Test
        @DisplayName("startDate=2024-03-10 → T3, T4, T5, T6 (inclusive)")
        void byStartDate_inclusive() {
            LocalDateTime start = LocalDateTime.of(2024, 3, 10, 0, 0);
            assertThat(ids(query(null, null, null, null, start, null, null, null, null)))
                    .containsExactlyInAnyOrder(t3, t4, t5, t6);
        }

        @Test
        @DisplayName("endDate=2024-03-10 → T1, T2, T3 (inclusive)")
        void byEndDate_inclusive() {
            LocalDateTime end = LocalDateTime.of(2024, 3, 10, 23, 59, 59);
            assertThat(ids(query(null, null, null, null, null, end, null, null, null)))
                    .containsExactlyInAnyOrder(t1, t2, t3);
        }

        @Test
        @DisplayName("date range 2024-02 through 2024-04 → T2, T3, T4")
        void byDateRange() {
            LocalDateTime start = LocalDateTime.of(2024, 2, 1,  0,  0);
            LocalDateTime end   = LocalDateTime.of(2024, 4, 30, 23, 59);
            assertThat(ids(query(null, null, null, null, start, end, null, null, null)))
                    .containsExactlyInAnyOrder(t2, t3, t4);
        }
    }

    // ===========================================================================
    // Seller filters — the implicit-join risk zone
    // ===========================================================================
    @Nested
    @DisplayName("Seller filters — implicit join correctness")
    class SellerFilters {

        @Test
        @DisplayName("companyId=CompanyA → T2, T6 only")
        void byCompanyA_returnsOnlyMatchingRows() {
            assertThat(ids(query(null, null, null, null, null, null, companyA.getId(), null, null)))
                    .containsExactlyInAnyOrder(t2, t6);
        }

        @Test
        @DisplayName("companyId=CompanyA → does NOT return T1, T3, T4 (null seller) or T5 (wrong company)")
        void byCompanyA_excludesNullSellerAndWrongCompany() {
            List<Long> result = ids(query(null, null, null, null, null, null, companyA.getId(), null, null));

            assertThat(result).doesNotContain(t1); // null sellerCompany
            assertThat(result).doesNotContain(t3); // null sellerCompany
            assertThat(result).doesNotContain(t4); // sellerContact (not company)
            assertThat(result).doesNotContain(t5); // CompanyB, not CompanyA
        }

        @Test
        @DisplayName("companyId=CompanyB → T5 only (one match among six rows with mixed null sellers)")
        void byCompanyB_singleResult_nullSellersNotLeaking() {
            // This is the critical implicit-join test:
            // T1, T3, T4 have null sellerCompany — they must NOT appear.
            // T2, T6 have CompanyA — they must NOT appear.
            // Only T5 (CompanyB) should be returned.
            List<Long> result = ids(query(null, null, null, null, null, null, companyB.getId(), null, null));

            assertThat(result).containsExactlyInAnyOrder(t5);
        }

        @Test
        @DisplayName("contactId=ContactA → T4 only")
        void byContactA_returnsOnlyMatchingRow() {
            assertThat(ids(query(null, null, null, null, null, null, null, contactA.getId(), null)))
                    .containsExactlyInAnyOrder(t4);
        }

        @Test
        @DisplayName("contactId=ContactA → does NOT return rows with null sellerContact or company sellers")
        void byContactA_excludesNullContactAndCompanySellers() {
            List<Long> result = ids(query(null, null, null, null, null, null, null, contactA.getId(), null));

            assertThat(result).doesNotContain(t1, t2, t3, t5, t6);
        }

        @Test
        @DisplayName("companyId + contactId both set → empty (no row can have both sellers simultaneously)")
        void companyAndContactBothSet_returnsEmpty() {
            // The service clears one seller when the other is set (see TransactionService)
            // so no transaction can satisfy both predicates — filtering by both must return nothing.
            List<Long> result = ids(query(null, null, null, null, null, null,
                    companyA.getId(), contactA.getId(), null));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("no companyId filter → rows with null seller ARE returned")
        void noCompanyFilter_nullSellersIncluded() {
            // When companyId is null the predicate is not added at all,
            // so T1 and T3 (null sellers) must appear.
            List<Long> result = ids(query(null, null, null, null, null, null, null, null, null));

            assertThat(result).contains(t1, t3);
        }
    }

    // ===========================================================================
    // Description search
    // ===========================================================================
    @Nested
    @DisplayName("Description search — case-insensitive LIKE")
    class DescriptionSearch {

        @Test
        @DisplayName("exact match returns the one matching row")
        void exactMatch() {
            assertThat(ids(query(null, null, null, null, null, null, null, null, "Invoice payment")))
                    .containsExactlyInAnyOrder(t1);
        }

        @Test
        @DisplayName("partial lowercase 'cable' matches T2 and T6")
        void partialLowercase() {
            assertThat(ids(query(null, null, null, null, null, null, null, null, "cable")))
                    .containsExactlyInAnyOrder(t2, t6);
        }

        @Test
        @DisplayName("partial UPPERCASE 'CABLE' still matches T2 and T6")
        void partialUppercase() {
            assertThat(ids(query(null, null, null, null, null, null, null, null, "CABLE")))
                    .containsExactlyInAnyOrder(t2, t6);
        }

        @Test
        @DisplayName("mixed case 'cAbLe' still matches T2 and T6")
        void partialMixedCase() {
            assertThat(ids(query(null, null, null, null, null, null, null, null, "cAbLe")))
                    .containsExactlyInAnyOrder(t2, t6);
        }

        @Test
        @DisplayName("blank string '' skips the predicate and returns all rows")
        void blankString_returnsAll() {
            assertThat(ids(query(null, null, null, null, null, null, null, null, "")))
                    .containsExactlyInAnyOrder(t1, t2, t3, t4, t5, t6);
        }

        @Test
        @DisplayName("null description skips the predicate and returns all rows")
        void nullDescription_returnsAll() {
            assertThat(ids(query(null, null, null, null, null, null, null, null, null)))
                    .containsExactlyInAnyOrder(t1, t2, t3, t4, t5, t6);
        }

        @Test
        @DisplayName("term that matches no description returns empty list")
        void noMatch_returnsEmpty() {
            assertThat(query(null, null, null, null, null, null, null, null, "xyz-no-match"))
                    .isEmpty();
        }
    }

    // ===========================================================================
    // Combined filters (AND logic)
    // ===========================================================================
    @Nested
    @DisplayName("Combined filters — AND logic")
    class CombinedFilters {

        @Test
        @DisplayName("projectA + EXPENSE → T2, T6")
        void projectAndType() {
            assertThat(ids(query(projectA.getId(), TransactionType.EXPENSE, null, null, null, null, null, null, null)))
                    .containsExactlyInAnyOrder(t2, t6);
        }

        @Test
        @DisplayName("projectA + EXPENSE + MATERIAL → T2, T6")
        void projectAndTypeAndCategory() {
            assertThat(ids(query(projectA.getId(), TransactionType.EXPENSE, TransactionCategory.MATERIAL,
                    null, null, null, null, null, null)))
                    .containsExactlyInAnyOrder(t2, t6);
        }

        @Test
        @DisplayName("EXPENSE + CASH → T2, T5, T6")
        void typeAndPaymentMethod() {
            assertThat(ids(query(null, TransactionType.EXPENSE, null, PaymentMethod.CASH,
                    null, null, null, null, null)))
                    .containsExactlyInAnyOrder(t2, t5, t6);
        }

        @Test
        @DisplayName("companyA + projectA → T2, T6 (CompanyA is only in Alpha)")
        void companyAndProject() {
            assertThat(ids(query(projectA.getId(), null, null, null, null, null, companyA.getId(), null, null)))
                    .containsExactlyInAnyOrder(t2, t6);
        }

        @Test
        @DisplayName("contactA + projectA → T4 only")
        void contactAndProject() {
            assertThat(ids(query(projectA.getId(), null, null, null, null, null, null, contactA.getId(), null)))
                    .containsExactlyInAnyOrder(t4);
        }

        @Test
        @DisplayName("companyA + MATERIAL → T2, T6")
        void companyAndCategory() {
            assertThat(ids(query(null, null, TransactionCategory.MATERIAL, null, null, null, companyA.getId(), null, null)))
                    .containsExactlyInAnyOrder(t2, t6);
        }

        @Test
        @DisplayName("type=EXPENSE + description='cable' → T2, T6")
        void typeAndDescription() {
            assertThat(ids(query(null, TransactionType.EXPENSE, null, null, null, null, null, null, "cable")))
                    .containsExactlyInAnyOrder(t2, t6);
        }

        @Test
        @DisplayName("date range Feb-Apr + EXPENSE → T2, T3")
        void dateRangeAndType() {
            LocalDateTime start = LocalDateTime.of(2024, 2, 1,  0,  0);
            LocalDateTime end   = LocalDateTime.of(2024, 4, 30, 23, 59);
            assertThat(ids(query(null, TransactionType.EXPENSE, null, null, start, end, null, null, null)))
                    .containsExactlyInAnyOrder(t2, t3);
        }

        @Test
        @DisplayName("all six filters narrowed to a single row → T2 only")
        void allFilters_singleResult() {
            // T2: Alpha / EXPENSE / MATERIAL / CASH / 2024-02-20 / CompanyA / "Cable purchase"
            LocalDateTime start = LocalDateTime.of(2024, 2, 1,  0,  0);
            LocalDateTime end   = LocalDateTime.of(2024, 2, 28, 23, 59);

            assertThat(ids(query(
                    projectA.getId(),
                    TransactionType.EXPENSE,
                    TransactionCategory.MATERIAL,
                    PaymentMethod.CASH,
                    start, end,
                    companyA.getId(), null,
                    "cable")))
                    .containsExactlyInAnyOrder(t2);
        }
    }

    // ===========================================================================
    // Edge cases
    // ===========================================================================
    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("filter combination that matches nothing → returns empty list, not an exception")
        void noMatchReturnsEmpty() {
            // No transaction is both in ProjectA and in category SALARY
            assertThat(query(projectA.getId(), null, TransactionCategory.SALARY, null, null, null, null, null, null))
                    .isEmpty();
        }

        @Test
        @DisplayName("startDate == endDate (single instant) → only transactions exactly on that timestamp")
        void startDateEqualsEndDate_exactTimestampMatch() {
            // T2 date is 2024-02-20 14:00 exactly
            LocalDateTime exact = LocalDateTime.of(2024, 2, 20, 14, 0);
            List<Long> result = ids(query(null, null, null, null, exact, exact, null, null, null));

            assertThat(result).containsExactlyInAnyOrder(t2);
        }

        @Test
        @DisplayName("startDate one second after T3 → T3 excluded, later rows included")
        void startDate_justAfterBoundary_excludesT3() {
            LocalDateTime justAfterT3 = LocalDateTime.of(2024, 3, 10, 9, 0, 1);
            List<Long> result = ids(query(null, null, null, null, justAfterT3, null, null, null, null));

            assertThat(result).doesNotContain(t3);
            assertThat(result).containsExactlyInAnyOrder(t4, t5, t6);
        }

        @Test
        @DisplayName("endDate one second before T3 → T3 excluded, earlier rows included")
        void endDate_justBeforeBoundary_excludesT3() {
            LocalDateTime justBeforeT3 = LocalDateTime.of(2024, 3, 10, 8, 59, 59);
            List<Long> result = ids(query(null, null, null, null, null, justBeforeT3, null, null, null));

            assertThat(result).doesNotContain(t3);
            assertThat(result).containsExactlyInAnyOrder(t1, t2);
        }

        @Test
        @DisplayName("non-existent projectId → returns empty list")
        void unknownProjectId_returnsEmpty() {
            assertThat(query(999_999L, null, null, null, null, null, null, null, null))
                    .isEmpty();
        }

        @Test
        @DisplayName("non-existent companyId → returns empty list (no phantom rows from null sellers)")
        void unknownCompanyId_returnsEmpty_notNullSellerRows() {
            // Critical: must not return T1/T3/T4 which have null sellers
            assertThat(query(null, null, null, null, null, null, 999_999L, null, null))
                    .isEmpty();
        }

        @Test
        @DisplayName("non-existent contactId → returns empty list")
        void unknownContactId_returnsEmpty() {
            assertThat(query(null, null, null, null, null, null, null, 999_999L, null))
                    .isEmpty();
        }
    }

    // ===========================================================================
    // Helpers
    // ===========================================================================

    /** Executes the specification against the real H2 database. */
    private List<Transaction> query(Long projectId, TransactionType type, TransactionCategory category,
                                    PaymentMethod paymentMethod, LocalDateTime startDate, LocalDateTime endDate,
                                    Long companyId, Long contactId, String description) {
        return transactionRepository.findAll(
                TransactionSpecification.filterBy(
                        projectId, type, category, paymentMethod,
                        startDate, endDate, companyId, contactId, description));
    }

    private List<Long> ids(List<Transaction> list) {
        return list.stream().map(Transaction::getId).toList();
    }

    private Long save(User user, Project project,
                      TransactionType type, double amount, TransactionCategory category,
                      PaymentMethod paymentMethod, String description, LocalDateTime date,
                      Company company, Contact contact) {
        Transaction t = new Transaction();
        t.setUser(user);
        t.setProject(project);
        t.setType(type);
        t.setAmount(BigDecimal.valueOf(amount));
        t.setCategory(category);
        t.setPaymentMethod(paymentMethod);
        t.setDescription(description);
        t.setDate(date);
        t.setSellerCompany(company);
        t.setSellerContact(contact);
        return transactionRepository.save(t).getId();
    }

    private Project savedProject(String name) {
        Project p = new Project();
        p.setName(name);
        p.setActive(true);
        return projectRepository.save(p);
    }

    private Company savedCompany(String name, CompanyRole role) {
        Company c = new Company();
        c.setName(name);
        c.setRole(role);
        c.setActive(true);
        return companyRepository.save(c);
    }

    private Contact savedContact(String name, ContactRole role) {
        Contact c = new Contact();
        c.setName(name);
        c.setRole(role);
        c.setActive(true);
        return contactRepository.save(c);
    }
}