package com.els.crmsystem.dto.output;

public record ExchangeRateDto(
        String currency,      // "USD" or "EUR"
        double officialRate,  // NBU
        double marketBuy,     // PrivatBank Buy
        double marketSell     // PrivatBank Sell
) {}
