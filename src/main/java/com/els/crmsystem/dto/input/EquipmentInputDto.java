package com.els.crmsystem.dto.input;

public record EquipmentInputDto(
        String name,
        String serialNumber,
        Integer warrantyMonths,
        String notes
) {}
