package com.els.crmsystem.dto.output;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectOutputDto(
        Long id,
        String name,
        String description,
        boolean active,
        LocalDateTime createdDate,
        LocalDateTime finishDate,

        // --- RELATIONAL DATA FOR THE UI (Now with phones!) ---
        Long clientId,
        String clientName,
        String clientPhone,

        Long installerId,
        String installerName,
        String installerPhone,

        Long equipmentDealerId,
        String equipmentDealerName,
        String equipmentDealerPhone,

        // --- ADDRESS FIELDS ---
        String addressText,
        Double latitude,
        Double longitude,

        // --- ADDITIONAL CONTACTS ---
        List<ContactOutputDto> additionalContacts
) {}