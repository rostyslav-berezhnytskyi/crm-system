package com.els.crmsystem.service;

import com.els.crmsystem.dto.output.CompanyOutputDto;
import com.els.crmsystem.dto.output.ContactOutputDto;
import com.els.crmsystem.dto.output.TransactionOutputDto;
import com.els.crmsystem.enums.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for ExcelExportService.
 *
 * No Spring context — the class has no dependencies, so it is instantiated directly.
 * Each test calls the method under test, parses the returned byte[] with Apache POI,
 * and asserts on the actual cell values that end up in the spreadsheet.
 *
 * The most critical business rule under test:
 *   EXPENSE amounts must be written as NEGATIVE numbers so that Excel's SUM
 *   formula produces the correct running balance. Silent breakage here would
 *   corrupt every finance export without any error.
 */
@DisplayName("ExcelExportService — Unit Tests")
class ExcelExportServiceTest {

    private ExcelExportService service;

    @BeforeEach
    void setUp() {
        service = new ExcelExportService();
    }

    // ===========================================================================
    // exportTransactionsToExcel
    // ===========================================================================
    @Nested
    @DisplayName("exportTransactionsToExcel")
    class ExportTransactions {

        // --- structure ---

        @Test
        @DisplayName("produces a non-empty byte array that is a valid XLSX workbook")
        void producesValidXlsx() throws IOException {
            byte[] bytes = service.exportTransactionsToExcel(List.of(incomeDto(100.0)));

            assertThat(bytes).isNotEmpty();
            // If this doesn't throw, it is a valid XLSX file
            try (XSSFWorkbook wb = parse(bytes)) {
                assertThat(wb.getNumberOfSheets()).isEqualTo(1);
            }
        }

        @Test
        @DisplayName("sheet is named 'Транзакції'")
        void sheetName() throws IOException {
            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(List.of()))) {
                assertThat(wb.getSheetAt(0).getSheetName()).isEqualTo("Транзакції");
            }
        }

        @Test
        @DisplayName("header row contains all 8 expected column titles in order")
        void headerTitles() throws IOException {
            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(List.of()))) {
                Row header = wb.getSheetAt(0).getRow(0);

                assertThat(str(header, 0)).isEqualTo("Дата");
                assertThat(str(header, 1)).isEqualTo("Проєкт");
                assertThat(str(header, 2)).isEqualTo("Контрагент");
                assertThat(str(header, 3)).isEqualTo("Опис");
                assertThat(str(header, 4)).isEqualTo("Категорія");
                assertThat(str(header, 5)).isEqualTo("Тип");
                assertThat(str(header, 6)).isEqualTo("Сума (₴)");
                assertThat(str(header, 7)).isEqualTo("Спосіб оплати");
            }
        }

        @Test
        @DisplayName("empty list → only the header row exists (rowNum=1 in total)")
        void emptyList_onlyHeaderRow() throws IOException {
            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(List.of()))) {
                Sheet sheet = wb.getSheetAt(0);
                // lastRowNum is 0-based; 0 means only row 0 (the header) was written
                assertThat(sheet.getLastRowNum()).isEqualTo(0);
            }
        }

        @Test
        @DisplayName("one transaction → header row + one data row")
        void oneTransaction_twoRows() throws IOException {
            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(List.of(incomeDto(500.0))))) {
                assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(1);
            }
        }

        @Test
        @DisplayName("three transactions → header row + three data rows")
        void threeTransactions_fourRows() throws IOException {
            List<TransactionOutputDto> txns = List.of(
                    incomeDto(100.0), expenseDto(200.0), incomeDto(300.0));

            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(txns))) {
                assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(3);
            }
        }

        // --- THE KEY BUSINESS RULE: sign of amount ---

        @Test
        @DisplayName("INCOME amount is written as a POSITIVE number")
        void incomeAmount_isPositive() throws IOException {
            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(List.of(incomeDto(1500.50))))) {
                Row dataRow = wb.getSheetAt(0).getRow(1);
                assertThat(num(dataRow, 6)).isCloseTo(1500.50, within(0.001));
            }
        }

        @Test
        @DisplayName("EXPENSE amount is written as a NEGATIVE number (critical business rule)")
        void expenseAmount_isNegative() throws IOException {
            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(List.of(expenseDto(300.0))))) {
                Row dataRow = wb.getSheetAt(0).getRow(1);
                assertThat(num(dataRow, 6)).isCloseTo(-300.0, within(0.001));
            }
        }

        @Test
        @DisplayName("mixed list: INCOME is positive, EXPENSE is negative — in the correct rows")
        void mixedList_correctSignPerRow() throws IOException {
            List<TransactionOutputDto> txns = List.of(
                    incomeDto(1000.0),   // row 1 → +1000
                    expenseDto(400.0),   // row 2 → -400
                    incomeDto(250.0),    // row 3 → +250
                    expenseDto(75.50));  // row 4 → -75.50

            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(txns))) {
                Sheet sheet = wb.getSheetAt(0);

                assertThat(num(sheet.getRow(1), 6)).isCloseTo( 1000.0,  within(0.001));
                assertThat(num(sheet.getRow(2), 6)).isCloseTo( -400.0,  within(0.001));
                assertThat(num(sheet.getRow(3), 6)).isCloseTo(  250.0,  within(0.001));
                assertThat(num(sheet.getRow(4), 6)).isCloseTo(  -75.50, within(0.001));
            }
        }

        @Test
        @DisplayName("null amount → 0.0 in the cell (no NPE)")
        void nullAmount_writesZero() throws IOException {
            TransactionOutputDto dto = txn(TransactionType.EXPENSE, null,
                    TransactionCategory.MATERIAL, PaymentMethod.CASH,
                    "Null amount test", LocalDateTime.now(), "Project");

            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(List.of(dto)))) {
                assertThat(num(wb.getSheetAt(0).getRow(1), 6)).isCloseTo(0.0, within(0.001));
            }
        }

        // --- type label ---

        @Test
        @DisplayName("INCOME type → cell contains 'Дохід'")
        void incomeTypeLabel() throws IOException {
            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(List.of(incomeDto(100.0))))) {
                assertThat(str(wb.getSheetAt(0).getRow(1), 5)).isEqualTo("Дохід");
            }
        }

        @Test
        @DisplayName("EXPENSE type → cell contains 'Витрата'")
        void expenseTypeLabel() throws IOException {
            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(List.of(expenseDto(100.0))))) {
                assertThat(str(wb.getSheetAt(0).getRow(1), 5)).isEqualTo("Витрата");
            }
        }

        @Test
        @DisplayName("null type → cell falls back to 'Витрата' (matches the ternary default)")
        void nullTypeLabel_fallsBackToExpense() throws IOException {
            TransactionOutputDto dto = txn(null, BigDecimal.valueOf(100.0),
                    TransactionCategory.MATERIAL, PaymentMethod.CASH,
                    "No type", LocalDateTime.now(), "Project");

            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(List.of(dto)))) {
                assertThat(str(wb.getSheetAt(0).getRow(1), 5)).isEqualTo("Витрата");
            }
        }

        // --- date formatting ---

        @Test
        @DisplayName("date is formatted as dd-MM-yyyy")
        void dateFormattedCorrectly() throws IOException {
            LocalDateTime date = LocalDateTime.of(2024, 3, 5, 14, 30);
            TransactionOutputDto dto = txn(TransactionType.INCOME, BigDecimal.valueOf(100.0),
                    TransactionCategory.PAYMENT, PaymentMethod.IBAN,
                    "Date test", date, "Project");

            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(List.of(dto)))) {
                assertThat(str(wb.getSheetAt(0).getRow(1), 0)).isEqualTo("05-03-2024");
            }
        }

        @Test
        @DisplayName("null date → empty string in the date cell")
        void nullDate_writesEmptyString() throws IOException {
            TransactionOutputDto dto = txn(TransactionType.INCOME, BigDecimal.valueOf(100.0),
                    TransactionCategory.PAYMENT, PaymentMethod.IBAN,
                    "Null date", null, "Project");

            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(List.of(dto)))) {
                assertThat(str(wb.getSheetAt(0).getRow(1), 0)).isEmpty();
            }
        }

        // --- Ukrainian names from enums ---

        @Test
        @DisplayName("category cell contains the Ukrainian name from the enum")
        void category_ukrainianName() throws IOException {
            TransactionOutputDto dto = txn(TransactionType.EXPENSE, BigDecimal.valueOf(200.0),
                    TransactionCategory.SALARY, PaymentMethod.CASH,
                    "Salary", LocalDateTime.now(), "Project");

            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(List.of(dto)))) {
                assertThat(str(wb.getSheetAt(0).getRow(1), 4))
                        .isEqualTo(TransactionCategory.SALARY.getUkrainianName());
            }
        }

        @Test
        @DisplayName("paymentMethod cell contains the Ukrainian name from the enum")
        void paymentMethod_ukrainianName() throws IOException {
            TransactionOutputDto dto = txn(TransactionType.INCOME, BigDecimal.valueOf(500.0),
                    TransactionCategory.PAYMENT, PaymentMethod.IBAN,
                    "Bank transfer", LocalDateTime.now(), "Project");

            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(List.of(dto)))) {
                assertThat(str(wb.getSheetAt(0).getRow(1), 7))
                        .isEqualTo(PaymentMethod.IBAN.getUkrainianName());
            }
        }

        // --- null field safety ---

        @Test
        @DisplayName("null projectName → empty string in cell (no NPE)")
        void nullProjectName_emptyCell() throws IOException {
            TransactionOutputDto dto = new TransactionOutputDto(
                    1L, null, null, null, TransactionType.INCOME,
                    BigDecimal.valueOf(100.0), TransactionCategory.PAYMENT,
                    PaymentMethod.CASH, "desc", LocalDateTime.now(), null, null);

            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(List.of(dto)))) {
                assertThat(str(wb.getSheetAt(0).getRow(1), 1)).isEmpty();
            }
        }

        @Test
        @DisplayName("null sellerName → empty string in cell (no NPE)")
        void nullSellerName_emptyCell() throws IOException {
            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(List.of(incomeDto(100.0))))) {
                // incomeDto has sellerName=null
                assertThat(str(wb.getSheetAt(0).getRow(1), 2)).isEmpty();
            }
        }

        @Test
        @DisplayName("null description → empty string in cell (no NPE)")
        void nullDescription_emptyCell() throws IOException {
            TransactionOutputDto dto = txn(TransactionType.INCOME, BigDecimal.valueOf(100.0),
                    TransactionCategory.PAYMENT, PaymentMethod.CASH,
                    null, LocalDateTime.now(), "Project");

            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(List.of(dto)))) {
                assertThat(str(wb.getSheetAt(0).getRow(1), 3)).isEmpty();
            }
        }

        @Test
        @DisplayName("null category → empty string in cell (no NPE)")
        void nullCategory_emptyCell() throws IOException {
            TransactionOutputDto dto = new TransactionOutputDto(
                    1L, "Proj", null, null, TransactionType.INCOME,
                    BigDecimal.valueOf(100.0), null, PaymentMethod.CASH,
                    "desc", LocalDateTime.now(), null, null);

            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(List.of(dto)))) {
                assertThat(str(wb.getSheetAt(0).getRow(1), 4)).isEmpty();
            }
        }

        @Test
        @DisplayName("null paymentMethod → empty string in cell (no NPE)")
        void nullPaymentMethod_emptyCell() throws IOException {
            TransactionOutputDto dto = new TransactionOutputDto(
                    1L, "Proj", null, null, TransactionType.INCOME,
                    BigDecimal.valueOf(100.0), TransactionCategory.PAYMENT,
                    null, "desc", LocalDateTime.now(), null, null);

            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(List.of(dto)))) {
                assertThat(str(wb.getSheetAt(0).getRow(1), 7)).isEmpty();
            }
        }

        // --- full-row correctness ---

        @Test
        @DisplayName("full data row: all cells match the input DTO exactly")
        void fullRowData_allCellsCorrect() throws IOException {
            LocalDateTime date = LocalDateTime.of(2024, 7, 4, 9, 0);
            TransactionOutputDto dto = new TransactionOutputDto(
                    42L,
                    "Kyiv Office",
                    "Solar Supplies",
                    "/companies/5",
                    TransactionType.EXPENSE,
                    BigDecimal.valueOf(999.99),
                    TransactionCategory.EQUIPMENT,
                    PaymentMethod.IBAN,
                    "Inverter purchase",
                    date,
                    "receipt.pdf",
                    null);

            try (XSSFWorkbook wb = parse(service.exportTransactionsToExcel(List.of(dto)))) {
                Row row = wb.getSheetAt(0).getRow(1);

                assertThat(str(row, 0)).isEqualTo("04-07-2024");
                assertThat(str(row, 1)).isEqualTo("Kyiv Office");
                assertThat(str(row, 2)).isEqualTo("Solar Supplies");
                assertThat(str(row, 3)).isEqualTo("Inverter purchase");
                assertThat(str(row, 4)).isEqualTo(TransactionCategory.EQUIPMENT.getUkrainianName());
                assertThat(str(row, 5)).isEqualTo("Витрата");
                assertThat(num(row, 6)).isCloseTo(-999.99, within(0.001)); // EXPENSE → negative
                assertThat(str(row, 7)).isEqualTo(PaymentMethod.IBAN.getUkrainianName());
            }
        }
    }

    // ===========================================================================
    // exportCompaniesToExcel
    // ===========================================================================
    @Nested
    @DisplayName("exportCompaniesToExcel")
    class ExportCompanies {

        @Test
        @DisplayName("sheet is named 'Компанії'")
        void sheetName() throws IOException {
            try (XSSFWorkbook wb = parse(service.exportCompaniesToExcel(List.of()))) {
                assertThat(wb.getSheetAt(0).getSheetName()).isEqualTo("Компанії");
            }
        }

        @Test
        @DisplayName("header row contains all 6 expected column titles")
        void headerTitles() throws IOException {
            try (XSSFWorkbook wb = parse(service.exportCompaniesToExcel(List.of()))) {
                Row header = wb.getSheetAt(0).getRow(0);

                assertThat(str(header, 0)).isEqualTo("Назва компанії");
                assertThat(str(header, 1)).isEqualTo("Роль");
                assertThat(str(header, 2)).isEqualTo("Телефон");
                assertThat(str(header, 3)).isEqualTo("Email");
                assertThat(str(header, 4)).isEqualTo("Вебсайт");
                assertThat(str(header, 5)).isEqualTo("Нотатки");
            }
        }

        @Test
        @DisplayName("empty list → only header row")
        void emptyList_onlyHeader() throws IOException {
            try (XSSFWorkbook wb = parse(service.exportCompaniesToExcel(List.of()))) {
                assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(0);
            }
        }

        @Test
        @DisplayName("data row: all cells match the company DTO")
        void fullRowData_allCellsCorrect() throws IOException {
            CompanyOutputDto dto = new CompanyOutputDto(
                    1L, "Solar Supplies Ltd", CompanyRole.DEALER,
                    "https://solar.ua", "+38 050 000 0001",
                    "info@solar.ua", "Main equipment supplier",
                    true, List.of());

            try (XSSFWorkbook wb = parse(service.exportCompaniesToExcel(List.of(dto)))) {
                Row row = wb.getSheetAt(0).getRow(1);

                assertThat(str(row, 0)).isEqualTo("Solar Supplies Ltd");
                assertThat(str(row, 1)).isEqualTo(CompanyRole.DEALER.getUkrainianName());
                assertThat(str(row, 2)).isEqualTo("+38 050 000 0001");
                assertThat(str(row, 3)).isEqualTo("info@solar.ua");
                assertThat(str(row, 4)).isEqualTo("https://solar.ua");
                assertThat(str(row, 5)).isEqualTo("Main equipment supplier");
            }
        }

        @Test
        @DisplayName("null optional fields → empty string in cells (no NPE)")
        void nullFields_emptyCells() throws IOException {
            CompanyOutputDto dto = new CompanyOutputDto(
                    2L, "Minimal Co", CompanyRole.OTHER,
                    null, null, null, null,
                    true, List.of());

            try (XSSFWorkbook wb = parse(service.exportCompaniesToExcel(List.of(dto)))) {
                Row row = wb.getSheetAt(0).getRow(1);

                assertThat(str(row, 2)).isEmpty(); // phone
                assertThat(str(row, 3)).isEmpty(); // email
                assertThat(str(row, 4)).isEmpty(); // website
                assertThat(str(row, 5)).isEmpty(); // notes
            }
        }

        @Test
        @DisplayName("null role → empty string in role cell (no NPE)")
        void nullRole_emptyCell() throws IOException {
            CompanyOutputDto dto = new CompanyOutputDto(
                    3L, "No Role Co", null, null, null, null, null, true, List.of());

            try (XSSFWorkbook wb = parse(service.exportCompaniesToExcel(List.of(dto)))) {
                assertThat(str(wb.getSheetAt(0).getRow(1), 1)).isEmpty();
            }
        }

        @Test
        @DisplayName("multiple companies → correct row count")
        void multipleCompanies_correctRowCount() throws IOException {
            List<CompanyOutputDto> list = List.of(
                    new CompanyOutputDto(1L, "A", CompanyRole.DEALER, null, null, null, null, true, List.of()),
                    new CompanyOutputDto(2L, "B", CompanyRole.CLIENT, null, null, null, null, true, List.of()),
                    new CompanyOutputDto(3L, "C", CompanyRole.OTHER,  null, null, null, null, true, List.of()));

            try (XSSFWorkbook wb = parse(service.exportCompaniesToExcel(list))) {
                assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(3); // header + 3 data rows
            }
        }
    }

    // ===========================================================================
    // exportContactsToExcel
    // ===========================================================================
    @Nested
    @DisplayName("exportContactsToExcel")
    class ExportContacts {

        @Test
        @DisplayName("sheet is named 'Контакти'")
        void sheetName() throws IOException {
            try (XSSFWorkbook wb = parse(service.exportContactsToExcel(List.of()))) {
                assertThat(wb.getSheetAt(0).getSheetName()).isEqualTo("Контакти");
            }
        }

        @Test
        @DisplayName("header row contains all 6 expected column titles")
        void headerTitles() throws IOException {
            try (XSSFWorkbook wb = parse(service.exportContactsToExcel(List.of()))) {
                Row header = wb.getSheetAt(0).getRow(0);

                assertThat(str(header, 0)).isEqualTo("Ім'я");
                assertThat(str(header, 1)).isEqualTo("Роль");
                assertThat(str(header, 2)).isEqualTo("Компанія");
                assertThat(str(header, 3)).isEqualTo("Телефон");
                assertThat(str(header, 4)).isEqualTo("Email");
                assertThat(str(header, 5)).isEqualTo("Нотатки");
            }
        }

        @Test
        @DisplayName("empty list → only header row")
        void emptyList_onlyHeader() throws IOException {
            try (XSSFWorkbook wb = parse(service.exportContactsToExcel(List.of()))) {
                assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(0);
            }
        }

        @Test
        @DisplayName("data row: all cells match the contact DTO")
        void fullRowData_allCellsCorrect() throws IOException {
            ContactOutputDto dto = new ContactOutputDto(
                    1L, 5L, "Solar Supplies Ltd", "Ivan Petrenko",
                    ContactRole.MANAGER, "+38 067 123 4567",
                    "ivan@solar.ua", "Key account manager",
                    true, null);

            try (XSSFWorkbook wb = parse(service.exportContactsToExcel(List.of(dto)))) {
                Row row = wb.getSheetAt(0).getRow(1);

                assertThat(str(row, 0)).isEqualTo("Ivan Petrenko");
                assertThat(str(row, 1)).isEqualTo(ContactRole.MANAGER.getUkrainianName());
                assertThat(str(row, 2)).isEqualTo("Solar Supplies Ltd");
                assertThat(str(row, 3)).isEqualTo("+38 067 123 4567");
                assertThat(str(row, 4)).isEqualTo("ivan@solar.ua");
                assertThat(str(row, 5)).isEqualTo("Key account manager");
            }
        }

        @Test
        @DisplayName("null companyName → '- Незалежний -' (freelancer fallback label)")
        void nullCompanyName_freelancerLabel() throws IOException {
            ContactOutputDto dto = new ContactOutputDto(
                    2L, null, null, "Freelancer Fred",
                    ContactRole.INSTALLER, null, null, null, true, null);

            try (XSSFWorkbook wb = parse(service.exportContactsToExcel(List.of(dto)))) {
                assertThat(str(wb.getSheetAt(0).getRow(1), 2)).isEqualTo("- Незалежний -");
            }
        }

        @Test
        @DisplayName("non-null companyName → company name is used (not the fallback)")
        void nonNullCompanyName_usesCompanyName() throws IOException {
            ContactOutputDto dto = new ContactOutputDto(
                    3L, 10L, "Actual Company", "Employee",
                    ContactRole.CLIENT, null, null, null, true, null);

            try (XSSFWorkbook wb = parse(service.exportContactsToExcel(List.of(dto)))) {
                assertThat(str(wb.getSheetAt(0).getRow(1), 2)).isEqualTo("Actual Company");
            }
        }

        @Test
        @DisplayName("null optional fields → empty string in cells (no NPE)")
        void nullFields_emptyCells() throws IOException {
            ContactOutputDto dto = new ContactOutputDto(
                    4L, null, null, "Minimal Contact",
                    ContactRole.OTHER, null, null, null, true, null);

            try (XSSFWorkbook wb = parse(service.exportContactsToExcel(List.of(dto)))) {
                Row row = wb.getSheetAt(0).getRow(1);

                assertThat(str(row, 3)).isEmpty(); // phone
                assertThat(str(row, 4)).isEmpty(); // email
                assertThat(str(row, 5)).isEmpty(); // notes
            }
        }

        @Test
        @DisplayName("null role → empty string in role cell (no NPE)")
        void nullRole_emptyCell() throws IOException {
            ContactOutputDto dto = new ContactOutputDto(
                    5L, null, null, "No Role Contact",
                    null, null, null, null, true, null);

            try (XSSFWorkbook wb = parse(service.exportContactsToExcel(List.of(dto)))) {
                assertThat(str(wb.getSheetAt(0).getRow(1), 1)).isEmpty();
            }
        }

        @Test
        @DisplayName("multiple contacts → correct row count")
        void multipleContacts_correctRowCount() throws IOException {
            List<ContactOutputDto> list = List.of(
                    contact("Alice", ContactRole.CLIENT),
                    contact("Bob",   ContactRole.DEALER),
                    contact("Carol", ContactRole.INSTALLER));

            try (XSSFWorkbook wb = parse(service.exportContactsToExcel(list))) {
                assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(3);
            }
        }
    }

    // ===========================================================================
    // Helpers
    // ===========================================================================

    private XSSFWorkbook parse(byte[] bytes) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    /**
     * Reads a cell as a String regardless of whether POI stored it as STRING or NUMERIC.
     * Returns empty string if the cell is null or blank.
     */
    private String str(Row row, int col) {
        if (row == null) return "";
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BLANK   -> "";
            default      -> cell.toString();
        };
    }

    /** Reads a numeric cell value. */
    private double num(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return 0.0;
        return cell.getNumericCellValue();
    }

    // --- DTO builders ---

    private TransactionOutputDto incomeDto(double amount) {
        return txn(TransactionType.INCOME, BigDecimal.valueOf(amount),
                TransactionCategory.PAYMENT, PaymentMethod.CASH,
                "Test income", LocalDateTime.of(2024, 1, 1, 10, 0), "Test Project");
    }

    private TransactionOutputDto expenseDto(double amount) {
        return txn(TransactionType.EXPENSE, BigDecimal.valueOf(amount),
                TransactionCategory.MATERIAL, PaymentMethod.CASH,
                "Test expense", LocalDateTime.of(2024, 1, 1, 10, 0), "Test Project");
    }

    private TransactionOutputDto txn(TransactionType type, BigDecimal amount,
                                     TransactionCategory category, PaymentMethod paymentMethod,
                                     String description, LocalDateTime date, String projectName) {
        return new TransactionOutputDto(
                1L, projectName, null, null,
                type, amount, category, paymentMethod,
                description, date, null, null);
    }

    private ContactOutputDto contact(String name, ContactRole role) {
        return new ContactOutputDto(null, null, null, name, role, null, null, null, true, null);
    }
}