package com.els.crmsystem.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    CASH("Cash", "Готівка"),
    CARD_PERSONAL("Personal Card", "Особиста картка"),
    CARD_COMPANY("Company Card", "Корпоративна картка"),
    IBAN("Bank Transfer", "Безготівковий розрахунок (IBAN)");

    private final String englishName;
    private final String ukrainianName;
}