package com.els.crmsystem.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransactionCategory {

    // 1. CONSTANT | 2. English (Logs) | 3. Ukrainian (UI) | 4. Description (Tooltip)

    // --- INCOME ---
    PREPAYMENT(
            TransactionType.INCOME,
            "Prepayment",
            "Аванс (Передоплата)",
            "Часткова оплата перед початком робіт"),

    PAYMENT(
            TransactionType.INCOME,
            "Payment",
            "Оплата / Доплата",
            "Фінальний розрахунок або оплата по факту"
    ),

    FINANCIAL_AID_IN(
            TransactionType.INCOME, // Change to INCOME
            "Financial Aid (Received)",
            "Отримання фін. допомоги",
            "Внесення власних коштів засновника"
    ),

    INTERNAL_TRANSFER_IN(
            TransactionType.INCOME,
            "Internal Transfer (In)",
            "Внутрішній переказ (Вхід)",
            "Зарахування коштів з іншого рахунку компанії"
    ),

    OTHER_INCOME(
            TransactionType.INCOME,
            "Other Income",
            "Інші надходження",
            "Повернення коштів, бонуси, кешбек тощо"
    ),


    // --- EXPENSES ---
    MATERIAL(
            TransactionType.EXPENSE,
            "Materials",
            "Матеріали",
            "Автомати, кабелі, гофри, щитки та розхідні матеріали"
    ),

    EQUIPMENT(
            TransactionType.EXPENSE,
            "Equipment",
            "Обладнання",
            "Інвертори, акумулятори (AKБ), сонячні панелі, генератори"
    ),

    TOOLS(
            TransactionType.EXPENSE,
            "Tools",
            "Інструменти",
            "Перфоратори, викрутки, драбини (те, що стає на баланс фірми)"
    ),

    TRANSPORT(
            TransactionType.EXPENSE,
            "Transport",
            "Транспорт",
            "Дизель, бензин, таксі або послуги доставки"
    ),

    SALARY(
            TransactionType.EXPENSE,
            "Salary",
            "Зарплата/Послуги",
            "Оплата праці монтажників або підрядників"
    ),

    OFFICE(
            TransactionType.EXPENSE,
            "Office",
            "Офісні витрати",
            "Оренда, кава, папір, інтернет"
    ),

    BANK_FEE(
            TransactionType.EXPENSE,
            "Bank Fees",
            "Банківські комісії",
            "Комісія за переказ, зняття готівки, обслуговування рахунку"
    ),

    TAXES(
            TransactionType.EXPENSE,
            "Taxes",
            "Податки",
            "Єдиний податок, ЄСВ, військові збори"
    ),

    REFUND_TO_CLIENT(
            TransactionType.EXPENSE, // Change to EXPENSE
            "Refund to Client",
            "Повернення коштів клієнту",
            "Помилковий платіж або повернення товару"
    ),

    INTERNAL_TRANSFER_OUT(
            TransactionType.EXPENSE, // Change to EXPENSE (Logic: Money leaves this specific account)
            "Internal Transfer",
            "Внутрішній переказ",
            "Зняття готівки або переказ на інший рахунок"
    ),

    FINANCIAL_AID_OUT(
            TransactionType.EXPENSE, // Change to EXPENSE
            "Financial Aid (Returned)",
            "Повернення фін. допомоги",
            "Виплата допомоги засновнику"
    ),

    OTHER_EXPENSE(
            TransactionType.EXPENSE,
            "Other",
            "Інше",
            "Все, що не підпадає під інші категорії"
    );



    private final TransactionType type;
    private final String englishName;
    private final String ukrainianName;
    private final String description;
}
