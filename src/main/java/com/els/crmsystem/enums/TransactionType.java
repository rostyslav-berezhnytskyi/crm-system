package com.els.crmsystem.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransactionType {
    INCOME("Income", "Дохід (Надходження)"),
    EXPENSE("Expense", "Витрата");
    private final String englishName;
    private final String ukrainianName;
}
