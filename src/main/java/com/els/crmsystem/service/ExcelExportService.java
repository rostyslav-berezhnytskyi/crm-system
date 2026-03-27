package com.els.crmsystem.service;

import com.els.crmsystem.dto.output.TransactionOutputDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

    public byte[] exportTransactionsToExcel(List<TransactionOutputDto> transactions) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Транзакції");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

            // Create Header Row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Дата", "Проєкт", "Контрагент", "Опис", "Категорія", "Тип", "Сума (₴)", "Спосіб оплати"};

            // Header Style (Bold)
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Populate Data Rows
            int rowIdx = 1;
            for (TransactionOutputDto txn : transactions) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(txn.date() != null ? txn.date().format(dateFormatter) : "");
                row.createCell(1).setCellValue(txn.projectName() != null ? txn.projectName() : "");
                row.createCell(2).setCellValue(txn.sellerName() != null ? txn.sellerName() : "");
                row.createCell(3).setCellValue(txn.description() != null ? txn.description() : "");
                row.createCell(4).setCellValue(txn.category() != null ? txn.category().getUkrainianName() : "");

                // Type: INCOME or EXPENSE
                String typeStr = txn.type() != null && txn.type().name().equals("INCOME") ? "Дохід" : "Витрата";
                row.createCell(5).setCellValue(typeStr);

                // Amount
                if (txn.amount() != null) {
                    // Make expenses negative in Excel for easy auto-sum
                    double amount = txn.amount().doubleValue();
                    if (txn.type() != null && txn.type().name().equals("EXPENSE")) {
                        amount = -amount;
                    }
                    row.createCell(6).setCellValue(amount);
                } else {
                    row.createCell(6).setCellValue(0.0);
                }

                row.createCell(7).setCellValue(txn.paymentMethod() != null ? txn.paymentMethod().getUkrainianName() : "");
            }

            // Auto-size columns for better readability
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Помилка під час створення Excel файлу", e);
        }
    }
}
