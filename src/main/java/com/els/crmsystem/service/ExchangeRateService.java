package com.els.crmsystem.service;

import com.els.crmsystem.dto.output.ExchangeRateDto;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ExchangeRateService {

    private final RestTemplate restTemplate = new RestTemplate();

    // In-memory cache
    private List<ExchangeRateDto> cachedRates = new ArrayList<>();
    private LocalDateTime lastFetchTime = LocalDateTime.MIN;

    public List<ExchangeRateDto> getCurrentRates() {
        // Fetch new rates only if cache is empty OR older than 1 hour
        if (cachedRates.isEmpty() || LocalDateTime.now().minusHours(1).isAfter(lastFetchTime)) {
            fetchRatesFromApis();
        }
        return cachedRates;
    }

    private void fetchRatesFromApis() {
        try {
            List<ExchangeRateDto> newRates = new ArrayList<>();

            // 1. Fetch NBU Official Rates
            String nbuUrl = "https://bank.gov.ua/NBUStatService/v1/statdirectory/exchange?json";
            Map[] nbuData = restTemplate.getForObject(nbuUrl, Map[].class);

            double nbuUsd = 0, nbuEur = 0;
            if (nbuData != null) {
                for (Map rate : nbuData) {
                    if ("USD".equals(rate.get("cc"))) nbuUsd = ((Number) rate.get("rate")).doubleValue();
                    if ("EUR".equals(rate.get("cc"))) nbuEur = ((Number) rate.get("rate")).doubleValue();
                }
            }

            // 2. Fetch Monobank Cash Rates (Much closer to the Street Market)
            String monoUrl = "https://api.monobank.ua/bank/currency";
            Map[] monoData = restTemplate.getForObject(monoUrl, Map[].class);

            double marketUsdBuy = 0, marketUsdSell = 0, marketEurBuy = 0, marketEurSell = 0;
            if (monoData != null) {
                for (Map rate : monoData) {
                    // Monobank uses official numeric currency codes (USD=840, EUR=978, UAH=980)
                    Number codeA = (Number) rate.get("currencyCodeA");
                    Number codeB = (Number) rate.get("currencyCodeB");

                    if (codeB != null && codeB.intValue() == 980) { // If exchanging against UAH
                        if (codeA != null && codeA.intValue() == 840) { // USD
                            marketUsdBuy = ((Number) rate.get("rateBuy")).doubleValue();
                            marketUsdSell = ((Number) rate.get("rateSell")).doubleValue();
                        }
                        if (codeA != null && codeA.intValue() == 978) { // EUR
                            marketEurBuy = ((Number) rate.get("rateBuy")).doubleValue();
                            marketEurSell = ((Number) rate.get("rateSell")).doubleValue();
                        }
                    }
                }
            }

            // 3. Package the data (Now using Monobank rates!)
            newRates.add(new ExchangeRateDto("USD", nbuUsd, marketUsdBuy, marketUsdSell));
            newRates.add(new ExchangeRateDto("EUR", nbuEur, marketEurBuy, marketEurSell));

            // 4. Safely update Cache
            this.cachedRates = newRates;
            this.lastFetchTime = LocalDateTime.now();

        } catch (Exception e) {
            System.err.println("Помилка оновлення курсів валют: " + e.getMessage());
            // If the internet is down, the system simply catches the error and continues using the old cached rates!
        }
    }
}
