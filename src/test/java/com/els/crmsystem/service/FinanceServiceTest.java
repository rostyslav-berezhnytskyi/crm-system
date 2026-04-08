package com.els.crmsystem.service;

import com.els.crmsystem.dto.output.TransactionOutputDto;
import com.els.crmsystem.enums.TransactionCategory;
import com.els.crmsystem.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("FinanceService")
class FinanceServiceTest {

    private FinanceService financeService;

    @BeforeEach
    void setUp() {
        financeService = new FinanceService();
    }

    // ---------------------------------------------------------------------------
    // Helper: build a minimal TransactionOutputDto (only type, amount, category matter here)
    // ---------------------------------------------------------------------------
    private TransactionOutputDto txn(TransactionType type, BigDecimal amount, TransactionCategory category) {
        return new TransactionOutputDto(null, null, null, null,
                type, amount, category, null, null, null, null, null);
    }

    private TransactionOutputDto income(double amount) {
        return txn(TransactionType.INCOME, BigDecimal.valueOf(amount), TransactionCategory.PAYMENT);
    }

    private TransactionOutputDto expense(double amount, TransactionCategory category) {
        return txn(TransactionType.EXPENSE, BigDecimal.valueOf(amount), category);
    }

    private TransactionOutputDto expense(double amount) {
        return expense(amount, TransactionCategory.MATERIAL);
    }

    // ===========================================================================
    // calculateTotalBalance
    // ===========================================================================
    @Nested
    @DisplayName("calculateTotalBalance")
    class CalculateTotalBalance {

        @Test
        @DisplayName("null list returns 0.0")
        void nullList() {
            assertThat(financeService.calculateTotalBalance(null)).isZero();
        }

        @Test
        @DisplayName("empty list returns 0.0")
        void emptyList() {
            assertThat(financeService.calculateTotalBalance(Collections.emptyList())).isZero();
        }

        @Test
        @DisplayName("single INCOME adds positively")
        void singleIncome() {
            assertThat(financeService.calculateTotalBalance(List.of(income(500.0))))
                    .isEqualTo(500.0);
        }

        @Test
        @DisplayName("single EXPENSE subtracts")
        void singleExpense() {
            assertThat(financeService.calculateTotalBalance(List.of(expense(200.0))))
                    .isEqualTo(-200.0);
        }

        @Test
        @DisplayName("mixed transactions: income minus expenses")
        void mixedTransactions() {
            List<TransactionOutputDto> txns = List.of(
                    income(1000.0),
                    expense(300.0),
                    income(500.0),
                    expense(150.0)
            );
            // 1000 + 500 - 300 - 150 = 1050
            assertThat(financeService.calculateTotalBalance(txns)).isEqualTo(1050.0);
        }

        @Test
        @DisplayName("all INCOME: balance equals total income")
        void allIncome() {
            List<TransactionOutputDto> txns = List.of(income(200.0), income(300.0), income(100.0));
            assertThat(financeService.calculateTotalBalance(txns)).isEqualTo(600.0);
        }

        @Test
        @DisplayName("all EXPENSE: balance is fully negative")
        void allExpense() {
            List<TransactionOutputDto> txns = List.of(expense(100.0), expense(50.0));
            assertThat(financeService.calculateTotalBalance(txns)).isEqualTo(-150.0);
        }

        @Test
        @DisplayName("null amount is treated as 0.0")
        void nullAmountTreatedAsZero() {
            TransactionOutputDto nullAmount = txn(TransactionType.INCOME, null, TransactionCategory.PAYMENT);
            assertThat(financeService.calculateTotalBalance(List.of(nullAmount))).isZero();
        }

        @Test
        @DisplayName("null amount EXPENSE does not reduce balance")
        void nullAmountExpenseNoEffect() {
            List<TransactionOutputDto> txns = List.of(
                    income(400.0),
                    txn(TransactionType.EXPENSE, null, TransactionCategory.MATERIAL)
            );
            assertThat(financeService.calculateTotalBalance(txns)).isEqualTo(400.0);
        }

        @Test
        @DisplayName("income exactly equals expense yields zero balance")
        void zeroNetBalance() {
            List<TransactionOutputDto> txns = List.of(income(500.0), expense(500.0));
            assertThat(financeService.calculateTotalBalance(txns)).isZero();
        }

        @Test
        @DisplayName("large number of mixed transactions sums correctly")
        void largeList() {
            List<TransactionOutputDto> txns = List.of(
                    income(10_000.0), expense(3_000.0), income(2_500.0),
                    expense(1_200.0), income(800.0), expense(600.0)
            );
            // 10000 + 2500 + 800 - 3000 - 1200 - 600 = 8500
            assertThat(financeService.calculateTotalBalance(txns))
                    .isCloseTo(8500.0, within(0.001));
        }
    }

    // ===========================================================================
    // calculateTotalIncome
    // ===========================================================================
    @Nested
    @DisplayName("calculateTotalIncome")
    class CalculateTotalIncome {

        @Test
        @DisplayName("null list returns 0.0")
        void nullList() {
            assertThat(financeService.calculateTotalIncome(null)).isZero();
        }

        @Test
        @DisplayName("empty list returns 0.0")
        void emptyList() {
            assertThat(financeService.calculateTotalIncome(Collections.emptyList())).isZero();
        }

        @Test
        @DisplayName("only EXPENSE transactions returns 0.0")
        void onlyExpenses() {
            List<TransactionOutputDto> txns = List.of(expense(100.0), expense(200.0));
            assertThat(financeService.calculateTotalIncome(txns)).isZero();
        }

        @Test
        @DisplayName("only INCOME transactions sums correctly")
        void onlyIncome() {
            List<TransactionOutputDto> txns = List.of(income(300.0), income(700.0));
            assertThat(financeService.calculateTotalIncome(txns)).isEqualTo(1000.0);
        }

        @Test
        @DisplayName("mixed: only INCOME entries are counted")
        void mixedOnlyCountsIncome() {
            List<TransactionOutputDto> txns = List.of(
                    income(500.0), expense(999.0), income(250.0)
            );
            assertThat(financeService.calculateTotalIncome(txns)).isEqualTo(750.0);
        }

        @Test
        @DisplayName("null amount INCOME is treated as 0.0")
        void nullAmountIncome() {
            List<TransactionOutputDto> txns = List.of(
                    txn(TransactionType.INCOME, null, TransactionCategory.PAYMENT),
                    income(100.0)
            );
            assertThat(financeService.calculateTotalIncome(txns)).isEqualTo(100.0);
        }

        @Test
        @DisplayName("single INCOME entry")
        void singleIncome() {
            assertThat(financeService.calculateTotalIncome(List.of(income(42.50)))).isEqualTo(42.50);
        }
    }

    // ===========================================================================
    // calculateTotalExpense
    // ===========================================================================
    @Nested
    @DisplayName("calculateTotalExpense")
    class CalculateTotalExpense {

        @Test
        @DisplayName("null list returns 0.0")
        void nullList() {
            assertThat(financeService.calculateTotalExpense(null)).isZero();
        }

        @Test
        @DisplayName("empty list returns 0.0")
        void emptyList() {
            assertThat(financeService.calculateTotalExpense(Collections.emptyList())).isZero();
        }

        @Test
        @DisplayName("only INCOME transactions returns 0.0")
        void onlyIncome() {
            List<TransactionOutputDto> txns = List.of(income(500.0), income(300.0));
            assertThat(financeService.calculateTotalExpense(txns)).isZero();
        }

        @Test
        @DisplayName("only EXPENSE transactions sums correctly")
        void onlyExpense() {
            List<TransactionOutputDto> txns = List.of(expense(100.0), expense(250.0));
            assertThat(financeService.calculateTotalExpense(txns)).isEqualTo(350.0);
        }

        @Test
        @DisplayName("mixed: only EXPENSE entries are counted")
        void mixedOnlyCountsExpense() {
            List<TransactionOutputDto> txns = List.of(
                    income(999.0), expense(400.0), expense(100.0)
            );
            assertThat(financeService.calculateTotalExpense(txns)).isEqualTo(500.0);
        }

        @Test
        @DisplayName("null amount EXPENSE is treated as 0.0")
        void nullAmountExpense() {
            List<TransactionOutputDto> txns = List.of(
                    txn(TransactionType.EXPENSE, null, TransactionCategory.MATERIAL),
                    expense(150.0)
            );
            assertThat(financeService.calculateTotalExpense(txns)).isEqualTo(150.0);
        }

        @Test
        @DisplayName("single EXPENSE entry")
        void singleExpense() {
            assertThat(financeService.calculateTotalExpense(List.of(expense(77.0)))).isEqualTo(77.0);
        }
    }

    // ===========================================================================
    // getExpenseBreakdownByCategory
    // ===========================================================================
    @Nested
    @DisplayName("getExpenseBreakdownByCategory")
    class GetExpenseBreakdownByCategory {

        @Test
        @DisplayName("null list returns empty map")
        void nullList() {
            assertThat(financeService.getExpenseBreakdownByCategory(null)).isEmpty();
        }

        @Test
        @DisplayName("empty list returns empty map")
        void emptyList() {
            assertThat(financeService.getExpenseBreakdownByCategory(Collections.emptyList())).isEmpty();
        }

        @Test
        @DisplayName("only INCOME transactions returns empty map")
        void onlyIncome() {
            List<TransactionOutputDto> txns = List.of(income(500.0), income(300.0));
            assertThat(financeService.getExpenseBreakdownByCategory(txns)).isEmpty();
        }

        @Test
        @DisplayName("single EXPENSE creates one entry in the map")
        void singleExpense() {
            List<TransactionOutputDto> txns = List.of(expense(200.0, TransactionCategory.SALARY));
            Map<TransactionCategory, Double> result = financeService.getExpenseBreakdownByCategory(txns);

            assertThat(result)
                    .hasSize(1)
                    .containsEntry(TransactionCategory.SALARY, 200.0);
        }

        @Test
        @DisplayName("multiple EXPENSE entries in same category are summed")
        void sameCategory() {
            List<TransactionOutputDto> txns = List.of(
                    expense(100.0, TransactionCategory.MATERIAL),
                    expense(250.0, TransactionCategory.MATERIAL),
                    expense(50.0,  TransactionCategory.MATERIAL)
            );
            Map<TransactionCategory, Double> result = financeService.getExpenseBreakdownByCategory(txns);

            assertThat(result)
                    .hasSize(1)
                    .containsEntry(TransactionCategory.MATERIAL, 400.0);
        }

        @Test
        @DisplayName("expenses in different categories are grouped separately")
        void multipleCategories() {
            List<TransactionOutputDto> txns = List.of(
                    expense(300.0, TransactionCategory.MATERIAL),
                    expense(150.0, TransactionCategory.TRANSPORT),
                    expense(200.0, TransactionCategory.SALARY)
            );
            Map<TransactionCategory, Double> result = financeService.getExpenseBreakdownByCategory(txns);

            assertThat(result)
                    .hasSize(3)
                    .containsEntry(TransactionCategory.MATERIAL,  300.0)
                    .containsEntry(TransactionCategory.TRANSPORT, 150.0)
                    .containsEntry(TransactionCategory.SALARY,    200.0);
        }

        @Test
        @DisplayName("INCOME transactions are excluded from the breakdown")
        void incomeIsExcluded() {
            List<TransactionOutputDto> txns = List.of(
                    income(1000.0),
                    expense(400.0, TransactionCategory.TOOLS),
                    income(500.0)
            );
            Map<TransactionCategory, Double> result = financeService.getExpenseBreakdownByCategory(txns);

            assertThat(result)
                    .hasSize(1)
                    .containsEntry(TransactionCategory.TOOLS, 400.0);
        }

        @Test
        @DisplayName("null amount EXPENSE is counted as 0.0 in the category")
        void nullAmountExpenseCountedAsZero() {
            List<TransactionOutputDto> txns = List.of(
                    txn(TransactionType.EXPENSE, null, TransactionCategory.OFFICE),
                    expense(100.0, TransactionCategory.OFFICE)
            );
            Map<TransactionCategory, Double> result = financeService.getExpenseBreakdownByCategory(txns);

            assertThat(result)
                    .hasSize(1)
                    .containsEntry(TransactionCategory.OFFICE, 100.0);
        }

        @Test
        @DisplayName("mixed categories with varying amounts")
        void mixedCategoriesMultipleEntries() {
            List<TransactionOutputDto> txns = List.of(
                    expense(500.0, TransactionCategory.MATERIAL),
                    expense(200.0, TransactionCategory.SALARY),
                    expense(300.0, TransactionCategory.MATERIAL),
                    expense(100.0, TransactionCategory.BANK_FEE),
                    income(999.0),                                        // must be ignored
                    expense(50.0,  TransactionCategory.BANK_FEE)
            );
            Map<TransactionCategory, Double> result = financeService.getExpenseBreakdownByCategory(txns);

            assertThat(result)
                    .hasSize(3)
                    .containsEntry(TransactionCategory.MATERIAL, 800.0)
                    .containsEntry(TransactionCategory.SALARY,   200.0)
                    .containsEntry(TransactionCategory.BANK_FEE, 150.0);
        }
    }
}