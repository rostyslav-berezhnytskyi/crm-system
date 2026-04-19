package com.els.crmsystem.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContactRole {
    CLIENT("Client", "Клієнт"),       // The customer (e.g., OSBB, private homeowner)
    DEALER("Dealer", "Дилер"),       // Where you buy equipment (inverters, batteries, cables)
    INSTALLER("Installer", "Монтажник"),    // External installation teams
    MANAGER("Manager", "Менеджер"),      // Project managers or partners
    OTHER("Other", "Інше"),         // Catch-all
    LEAD("Lead", "Лід"),             // Cold lead in the sales pipeline
    PROSPECT("Prospect", "Потенційний клієнт (Лід)"); // Warm prospect in the sales pipeline

    private final String englishName;
    private final String ukrainianName;
}
