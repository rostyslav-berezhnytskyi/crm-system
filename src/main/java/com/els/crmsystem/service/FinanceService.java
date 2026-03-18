package com.els.crmsystem.service;

import com.els.crmsystem.dto.output.TransactionOutputDto;
import com.els.crmsystem.enums.TransactionCategory;
import com.els.crmsystem.enums.TransactionType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class FinanceService {

    /**
     * Calculates the total balance from any given list of transactions.
     */
    public double calculateTotalBalance(List<TransactionOutputDto> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return 0.0;
        }

        return transactions.stream()
                .mapToDouble(txn -> {
                    double amount = txn.amount() != null ? txn.amount().doubleValue() : 0.0;
                    return txn.type() == TransactionType.INCOME ? amount : -amount;
                })
                .sum();
    }

    // Calculate ONLY the total money received
    public double calculateTotalIncome(List<TransactionOutputDto> transactions) {
        if (transactions == null || transactions.isEmpty()) return 0.0;

        return transactions.stream()
                .filter(txn -> txn.type() == TransactionType.INCOME)
                .mapToDouble(txn -> txn.amount() != null ? txn.amount().doubleValue() : 0.0)
                .sum();
    }

    // Calculate ONLY the total money spent
    public double calculateTotalExpense(List<TransactionOutputDto> transactions) {
        if (transactions == null || transactions.isEmpty()) return 0.0;

        return transactions.stream()
                .filter(txn -> txn.type() == TransactionType.EXPENSE)
                .mapToDouble(txn -> txn.amount() != null ? txn.amount().doubleValue() : 0.0)
                .sum();
    }

    // Group expenses by category and sum them up Returns a Map of Category -> Total Amount
    public Map<TransactionCategory, Double> getExpenseBreakdownByCategory(List<TransactionOutputDto> transactions) {
        if (transactions == null || transactions.isEmpty()) return java.util.Collections.emptyMap();

        return transactions.stream()
                .filter(txn -> txn.type() == com.els.crmsystem.enums.TransactionType.EXPENSE)
                .collect(java.util.stream.Collectors.groupingBy(
                        TransactionOutputDto::category,
                        java.util.stream.Collectors.summingDouble(txn -> txn.amount() != null ? txn.amount().doubleValue() : 0.0)
                ));
    }
}
