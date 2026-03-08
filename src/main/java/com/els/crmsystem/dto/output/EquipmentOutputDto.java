package com.els.crmsystem.dto.output;

public record EquipmentOutputDto(
        Long id,
        String name,
        String serialNumber,
        Integer warrantyMonths,
        String notes
) {}
