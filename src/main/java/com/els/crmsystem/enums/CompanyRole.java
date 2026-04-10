package com.els.crmsystem.enums;

import lombok.Getter;

@Getter
public enum CompanyRole {
    CLIENT("Клієнт"),           // e.g., OSBB Abalmasova
    DEALER("Постачальник"),     // e.g., Solarverse, Electricashop
    SUBCONTRACTOR("Підрядник"), // If you hire another company to do the roof mounting
    OTHER("Інше"),
    LEAD("Лід"),                // Cold lead in the sales pipeline
    PROSPECT("Потенційний клієнт"); // Warm prospect in the sales pipeline

    private final String ukrainianName;

    CompanyRole(String ukrainianName) {
        this.ukrainianName = ukrainianName;
    }
}
