package com.els.crmsystem.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContactRole {
    CLIENT("Client", "Клієнт"),       // The customer (e.g., OSBB, private homeowner)
    POTENTIAL_CLIENT("Potential Client", "Потенційний клієнт"),       // Potential client
    DEALER("Dealer", "Дилер"),       // Where you buy equipment (inverters, batteries, cables)
    INSTALLER("Installer", "Монтажник"),    // External installation teams
    MANAGER("Manager", "Менеджер"),      // Project managers or partners
    OTHER("Other", "Інше");         // Catch-all

    private final String englishName;
    private final String ukrainianName;
}
