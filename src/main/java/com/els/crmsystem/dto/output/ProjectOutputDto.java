package com.els.crmsystem.dto.output;

import java.time.LocalDateTime;

public record ProjectOutputDto(
        Long id,           // Server generated
        String name,
        String description,
        boolean active,
        LocalDateTime createdDate, // Server generated

        // --- RELATIONAL DATA FOR THE UI ---
        Long clientId,
        String clientName,
        Long installerId,
        String installerName,
        Long equipmentDealerId,
        String equipmentDealerName,

        // --- ADDRESS FIELDS ---
        String addressText,
        Double latitude,
        Double longitude
) {}