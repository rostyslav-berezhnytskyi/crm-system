package com.els.crmsystem.service;

import com.els.crmsystem.dto.output.ExchangeRateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExchangeRateService.
 *
 * ExchangeRateService creates its own RestTemplate inline:
 *   private final RestTemplate restTemplate = new RestTemplate();
 *
 * It cannot be constructor-injected, so we use Spring's ReflectionTestUtils
 * to swap the private field with a Mockito mock after instantiation.
 * The same utility is used to manipulate the private cache fields
 * (cachedRates, lastFetchTime) so we can test every branch of the
 * caching condition without sleeping or touching system time.
 *
 * Zero Spring context needed — pure JUnit 5 + Mockito.
 *
 * Caching condition (from the source):
 *   if (cachedRates.isEmpty() || LocalDateTime.now().minusHours(1).isAfter(lastFetchTime))
 *       → fetch from APIs
 *
 * "Stale" means lastFetchTime is older than 1 hour ago.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateService — Unit Tests")
class ExchangeRateServiceTest {

    @Mock
    RestTemplate mockRestTemplate;

    ExchangeRateService service;

    @BeforeEach
    void setUp() {
        service = new ExchangeRateService();
        // Inject the mock into the private field that was initialised inline
        ReflectionTestUtils.setField(service, "restTemplate", mockRestTemplate);
    }

    // ===========================================================================
    // Test data builders
    // ===========================================================================

    /**
     * Simulates the NBU JSON array. Each entry has "cc" (currency code) and "rate".
     * Includes PLN as a realistic noise entry that the service must ignore.
     */
    @SuppressWarnings("rawtypes")
    private Map[] nbuResponse(double usdRate, double eurRate) {
        return new Map[]{
                Map.of("cc", "USD", "rate", usdRate),
                Map.of("cc", "EUR", "rate", eurRate),
                Map.of("cc", "PLN", "rate", 10.5)  // must be ignored
        };
    }

    /**
     * Simulates the Monobank JSON array. Each entry has numeric currency codes
     * (840=USD, 978=EUR, 826=GBP) and rateBuy/rateSell against UAH (980).
     */
    @SuppressWarnings("rawtypes")
    private Map[] monoResponse(double usdBuy, double usdSell,
                               double eurBuy, double eurSell) {
        return new Map[]{
                Map.of("currencyCodeA", 840, "currencyCodeB", 980, // USD/UAH
                        "rateBuy", usdBuy, "rateSell", usdSell),
                Map.of("currencyCodeA", 978, "currencyCodeB", 980, // EUR/UAH
                        "rateBuy", eurBuy, "rateSell", eurSell),
                Map.of("currencyCodeA", 826, "currencyCodeB", 980, // GBP/UAH — must be ignored
                        "rateBuy", 51.0, "rateSell", 52.0),
                Map.of("currencyCodeA", 840, "currencyCodeB", 978, // USD/EUR — must be ignored (codeB != 980)
                        "rateBuy", 0.93, "rateSell", 0.94)
        };
    }

    /** Default API responses used in most tests. */
    @SuppressWarnings("rawtypes")
    private Map[] defaultNbu()  { return nbuResponse(41.5, 44.0); }
    @SuppressWarnings("rawtypes")
    private Map[] defaultMono() { return monoResponse(41.0, 41.8, 43.5, 44.5); }

    /**
     * Stubs both endpoints: NBU and Monobank are distinguished by URL substring.
     * Using thenAnswer keeps the stub reusable across multiple fetch cycles.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubSuccessfulApis() {
        when(mockRestTemplate.getForObject(anyString(), eq(Map[].class)))
                .thenAnswer(invocation -> {
                    String url = invocation.getArgument(0, String.class);
                    return url.contains("bank.gov.ua") ? defaultNbu() : defaultMono();
                });
    }

    // ---------------------------------------------------------------------------
    // Cache manipulation helpers
    // ---------------------------------------------------------------------------

    /** Forces the cache to appear stale (last fetched 2 hours ago). */
    private void makeStale() {
        ReflectionTestUtils.setField(service, "lastFetchTime",
                LocalDateTime.now().minusHours(2));
    }

    /**
     * Bypasses the service to directly pre-populate the cache with known data
     * and mark it as freshly fetched, so the next getCurrentRates() call
     * returns cached data unless we also call makeStale().
     */
    private void prePopulateCache(List<ExchangeRateDto> rates) {
        ReflectionTestUtils.setField(service, "cachedRates", new ArrayList<>(rates));
        ReflectionTestUtils.setField(service, "lastFetchTime", LocalDateTime.now());
    }

    /** Retrieves a named currency from the service result (fails fast if missing). */
    private ExchangeRateDto findCurrency(List<ExchangeRateDto> rates, String code) {
        return rates.stream()
                .filter(r -> code.equals(r.currency()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Currency not found in result: " + code));
    }

    // ===========================================================================
    // First call — always fetches
    // ===========================================================================
    @Nested
    @DisplayName("First call — always fetches from the API")
    class FirstCall {

        @BeforeEach
        void stub() { stubSuccessfulApis(); }

        @Test
        @DisplayName("triggers exactly 2 HTTP calls (one NBU + one Monobank)")
        void firstCall_triggersHttpFetch() {
            service.getCurrentRates();

            // Each fetchRatesFromApis() makes 2 getForObject calls
            verify(mockRestTemplate, times(2)).getForObject(anyString(), eq(Map[].class));
        }

        @Test
        @DisplayName("returns a non-empty list")
        void firstCall_returnsNonEmptyList() {
            assertThat(service.getCurrentRates()).isNotEmpty();
        }

        @Test
        @DisplayName("returns exactly 2 entries (USD and EUR only)")
        void firstCall_returnsExactlyTwoEntries() {
            assertThat(service.getCurrentRates()).hasSize(2);
        }

        @Test
        @DisplayName("result contains a USD entry")
        void firstCall_containsUsd() {
            assertThat(service.getCurrentRates())
                    .anyMatch(r -> "USD".equals(r.currency()));
        }

        @Test
        @DisplayName("result contains an EUR entry")
        void firstCall_containsEur() {
            assertThat(service.getCurrentRates())
                    .anyMatch(r -> "EUR".equals(r.currency()));
        }
    }

    // ===========================================================================
    // Rate parsing
    // ===========================================================================
    @Nested
    @DisplayName("Rate parsing — values extracted from API responses")
    class RateParsing {

        List<ExchangeRateDto> rates;

        @BeforeEach
        void fetchOnce() {
            stubSuccessfulApis();
            rates = service.getCurrentRates();
        }

        @Test
        @DisplayName("USD official (NBU) rate = 41.5")
        void usdOfficialRate() {
            assertThat(findCurrency(rates, "USD").officialRate())
                    .isCloseTo(41.5, within(0.001));
        }

        @Test
        @DisplayName("EUR official (NBU) rate = 44.0")
        void eurOfficialRate() {
            assertThat(findCurrency(rates, "EUR").officialRate())
                    .isCloseTo(44.0, within(0.001));
        }

        @Test
        @DisplayName("USD market buy rate (Monobank) = 41.0")
        void usdMarketBuy() {
            assertThat(findCurrency(rates, "USD").marketBuy())
                    .isCloseTo(41.0, within(0.001));
        }

        @Test
        @DisplayName("USD market sell rate (Monobank) = 41.8")
        void usdMarketSell() {
            assertThat(findCurrency(rates, "USD").marketSell())
                    .isCloseTo(41.8, within(0.001));
        }

        @Test
        @DisplayName("EUR market buy rate (Monobank) = 43.5")
        void eurMarketBuy() {
            assertThat(findCurrency(rates, "EUR").marketBuy())
                    .isCloseTo(43.5, within(0.001));
        }

        @Test
        @DisplayName("EUR market sell rate (Monobank) = 44.5")
        void eurMarketSell() {
            assertThat(findCurrency(rates, "EUR").marketSell())
                    .isCloseTo(44.5, within(0.001));
        }

        @Test
        @DisplayName("PLN entry in NBU response is ignored (not in result)")
        void plnFromNbu_isIgnored() {
            assertThat(rates).noneMatch(r -> "PLN".equals(r.currency()));
        }

        @Test
        @DisplayName("GBP/UAH Monobank entry is ignored (not in result)")
        void gbpFromMono_isIgnored() {
            // GBP (826) is present in the mock Monobank response but must not appear
            assertThat(rates).hasSize(2);
        }

        @Test
        @DisplayName("USD/EUR Monobank cross-rate (codeB != 980) is ignored")
        void nonUahBaseInMono_isIgnored() {
            // The mock includes a USD/EUR entry (codeB=978, not 980)
            // USD marketBuy must reflect only the USD/UAH entry (41.0), not the cross rate
            assertThat(findCurrency(rates, "USD").marketBuy())
                    .isCloseTo(41.0, within(0.001));
        }
    }

    // ===========================================================================
    // Caching — within 1 hour (cache is fresh)
    // ===========================================================================
    @Nested
    @DisplayName("Caching — second call within 1 hour uses cache, no HTTP call")
    class CachingFreshCache {

        @Test
        @DisplayName("second call within 1 hour makes no additional HTTP requests")
        void secondCall_withinOneHour_noAdditionalFetch() {
            stubSuccessfulApis();

            service.getCurrentRates(); // call 1: fetches (2 HTTP requests)
            service.getCurrentRates(); // call 2: must use cache (0 more HTTP requests)

            // Total must remain 2 (1 NBU + 1 Mono), not 4
            verify(mockRestTemplate, times(2)).getForObject(anyString(), eq(Map[].class));
        }

        @Test
        @DisplayName("second call within 1 hour returns the same list reference / equal data")
        void secondCall_withinOneHour_returnsSameData() {
            stubSuccessfulApis();

            List<ExchangeRateDto> first  = service.getCurrentRates();
            List<ExchangeRateDto> second = service.getCurrentRates();

            assertThat(second).isEqualTo(first);
        }

        @Test
        @DisplayName("10 rapid successive calls still produce only 1 fetch cycle (2 HTTP requests total)")
        void tenRapidCalls_onlyOneFetchCycle() {
            stubSuccessfulApis();

            for (int i = 0; i < 10; i++) {
                service.getCurrentRates();
            }

            verify(mockRestTemplate, times(2)).getForObject(anyString(), eq(Map[].class));
        }

        @Test
        @DisplayName("cache pre-populated as fresh → getCurrentRates() returns cached data, no HTTP call at all")
        void prePopulatedFreshCache_noHttpCall() {
            List<ExchangeRateDto> existing = List.of(
                    new ExchangeRateDto("USD", 38.0, 37.5, 38.5),
                    new ExchangeRateDto("EUR", 41.0, 40.5, 41.5));
            prePopulateCache(existing);

            List<ExchangeRateDto> result = service.getCurrentRates();

            assertThat(result).isEqualTo(existing);
            verifyNoInteractions(mockRestTemplate);
        }
    }

    // ===========================================================================
    // Caching — after 1 hour (cache is stale)
    // ===========================================================================
    @Nested
    @DisplayName("Caching — call after 1 hour triggers a new fetch")
    class CachingStaleCache {

        @Test
        @DisplayName("call after 1 hour makes 2 additional HTTP requests (re-fetch)")
        void callAfterOneHour_triggersReFetch() {
            stubSuccessfulApis();

            service.getCurrentRates(); // fetch #1
            makeStale();
            service.getCurrentRates(); // fetch #2

            // 2 fetches × 2 HTTP calls each = 4 total
            verify(mockRestTemplate, times(4)).getForObject(anyString(), eq(Map[].class));
        }

        @Test
        @DisplayName("call after 1 hour returns freshly fetched data, not the old stale values")
        @SuppressWarnings({"rawtypes", "unchecked"})
        void callAfterOneHour_returnsFreshData() {
            stubSuccessfulApis();
            service.getCurrentRates(); // fetch #1: USD = 41.5

            makeStale();

            // Override stub: second fetch returns updated rates
            when(mockRestTemplate.getForObject(anyString(), eq(Map[].class)))
                    .thenAnswer(inv -> {
                        String url = inv.getArgument(0, String.class);
                        return url.contains("bank.gov.ua")
                                ? nbuResponse(50.0, 55.0)   // new rates
                                : defaultMono();
                    });

            List<ExchangeRateDto> fresh = service.getCurrentRates();
            assertThat(findCurrency(fresh, "USD").officialRate())
                    .isCloseTo(50.0, within(0.001));
        }

        @Test
        @DisplayName("initial lastFetchTime = LocalDateTime.MIN → first call is always treated as stale")
        void initialState_alwaysStale_firstCallFetches() {
            // The field starts at LocalDateTime.MIN, which is definitely more than 1 hour ago
            stubSuccessfulApis();
            service.getCurrentRates();

            verify(mockRestTemplate, atLeastOnce()).getForObject(anyString(), eq(Map[].class));
        }

        @Test
        @DisplayName("stale empty cache → re-fetch populates cache from API response")
        void staleEmptyCache_reFetchPopulatesCache() {
            // Simulate a state where cache is non-empty but stale, then became empty somehow
            // Actually: test that makeStale() combined with empty cache triggers fetch
            // (the isEmpty() OR condition handles this — either condition triggers a fetch)
            stubSuccessfulApis();
            // cache is empty (default) — isEmpty() is true → fetch happens
            List<ExchangeRateDto> result = service.getCurrentRates();

            assertThat(result).hasSize(2);
        }
    }

    // ===========================================================================
    // Error handling
    // ===========================================================================
    @Nested
    @DisplayName("Error handling — API failures are silently swallowed")
    class ErrorHandling {

        @Test
        @DisplayName("API throws on first call → RuntimeException is NOT propagated")
        void apiThrowsOnFirstCall_exceptionSwallowed() {
            when(mockRestTemplate.getForObject(anyString(), eq(Map[].class)))
                    .thenThrow(new RuntimeException("Network error"));

            assertThatNoException().isThrownBy(() -> service.getCurrentRates());
        }

        @Test
        @DisplayName("API throws on first call → empty list returned (no cached data yet)")
        void apiThrowsOnFirstCall_emptyListReturned() {
            when(mockRestTemplate.getForObject(anyString(), eq(Map[].class)))
                    .thenThrow(new RuntimeException("Connection refused"));

            List<ExchangeRateDto> result = service.getCurrentRates();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("API throws when stale cache exists → old cached rates are returned intact")
        void apiThrows_withExistingStaleCache_oldRatesReturned() {
            List<ExchangeRateDto> staleRates = List.of(
                    new ExchangeRateDto("USD", 41.5, 41.0, 41.8),
                    new ExchangeRateDto("EUR", 44.0, 43.5, 44.5));
            prePopulateCache(staleRates);
            makeStale(); // force a re-fetch attempt

            when(mockRestTemplate.getForObject(anyString(), eq(Map[].class)))
                    .thenThrow(new RuntimeException("API down"));

            List<ExchangeRateDto> result = service.getCurrentRates();

            assertThat(result).isEqualTo(staleRates);
        }

        @Test
        @DisplayName("Monobank throws mid-fetch (after NBU succeeds) → old cache is preserved, not partial data")
        @SuppressWarnings({"rawtypes", "unchecked"})
        void monoThrowsMidFetch_cacheNotPartiallyUpdated() {
            List<ExchangeRateDto> existingCache = List.of(
                    new ExchangeRateDto("USD", 40.0, 39.5, 40.3));
            prePopulateCache(existingCache);
            makeStale();

            when(mockRestTemplate.getForObject(anyString(), eq(Map[].class)))
                    .thenAnswer(inv -> {
                        String url = inv.getArgument(0, String.class);
                        if (url.contains("bank.gov.ua")) return defaultNbu(); // NBU succeeds
                        throw new RuntimeException("Monobank is down");       // Mono fails
                    });

            List<ExchangeRateDto> result = service.getCurrentRates();

            // The cache must not be partially updated with only the NBU data
            assertThat(result).isEqualTo(existingCache);
        }

        @Test
        @DisplayName("repeated failures never crash the service — always returns last known cache")
        void repeatedFailures_serviceStaysStable() {
            List<ExchangeRateDto> lastGoodData = List.of(
                    new ExchangeRateDto("USD", 41.5, 41.0, 41.8));
            prePopulateCache(lastGoodData);

            when(mockRestTemplate.getForObject(anyString(), eq(Map[].class)))
                    .thenThrow(new RuntimeException("Persistent failure"));

            for (int i = 0; i < 5; i++) {
                makeStale(); // each iteration forces a re-fetch attempt
                assertThatNoException().isThrownBy(() -> {
                    List<ExchangeRateDto> result = service.getCurrentRates();
                    assertThat(result).isEqualTo(lastGoodData);
                });
            }
        }
    }

    // ===========================================================================
    // Null API responses
    // ===========================================================================
    @Nested
    @DisplayName("Null API responses — defensive defaults")
    class NullResponses {

        @Test
        @DisplayName("NBU returns null → officialRate defaults to 0.0, no exception")
        @SuppressWarnings({"rawtypes", "unchecked"})
        void nbuReturnsNull_officialRatesZero() {
            when(mockRestTemplate.getForObject(anyString(), eq(Map[].class)))
                    .thenAnswer(inv -> {
                        String url = inv.getArgument(0, String.class);
                        return url.contains("bank.gov.ua") ? null : defaultMono();
                    });

            List<ExchangeRateDto> result = service.getCurrentRates();

            assertThat(result).hasSize(2);
            assertThat(findCurrency(result, "USD").officialRate()).isCloseTo(0.0, within(0.001));
            assertThat(findCurrency(result, "EUR").officialRate()).isCloseTo(0.0, within(0.001));
        }

        @Test
        @DisplayName("Monobank returns null → market rates default to 0.0, no exception")
        @SuppressWarnings({"rawtypes", "unchecked"})
        void monoReturnsNull_marketRatesZero() {
            when(mockRestTemplate.getForObject(anyString(), eq(Map[].class)))
                    .thenAnswer(inv -> {
                        String url = inv.getArgument(0, String.class);
                        return url.contains("bank.gov.ua") ? defaultNbu() : null;
                    });

            List<ExchangeRateDto> result = service.getCurrentRates();

            assertThat(result).hasSize(2);
            ExchangeRateDto usd = findCurrency(result, "USD");
            assertThat(usd.marketBuy()).isCloseTo(0.0, within(0.001));
            assertThat(usd.marketSell()).isCloseTo(0.0, within(0.001));
        }

        @Test
        @DisplayName("both APIs return null → 2 entries with all rates = 0.0, no exception")
        void bothReturnNull_twoZeroEntries() {
            when(mockRestTemplate.getForObject(anyString(), eq(Map[].class)))
                    .thenReturn(null); // both NBU and Mono calls return null

            assertThatNoException().isThrownBy(() -> {
                List<ExchangeRateDto> result = service.getCurrentRates();

                assertThat(result).hasSize(2);
                result.forEach(r -> {
                    assertThat(r.officialRate()).isCloseTo(0.0, within(0.001));
                    assertThat(r.marketBuy()).isCloseTo(0.0,  within(0.001));
                    assertThat(r.marketSell()).isCloseTo(0.0, within(0.001));
                });
            });
        }

        @Test
        @DisplayName("Monobank returns null but NBU rates are still parsed correctly")
        @SuppressWarnings({"rawtypes", "unchecked"})
        void monoReturnsNull_nbuRatesStillParsed() {
            when(mockRestTemplate.getForObject(anyString(), eq(Map[].class)))
                    .thenAnswer(inv -> {
                        String url = inv.getArgument(0, String.class);
                        return url.contains("bank.gov.ua") ? defaultNbu() : null;
                    });

            List<ExchangeRateDto> result = service.getCurrentRates();

            assertThat(findCurrency(result, "USD").officialRate()).isCloseTo(41.5, within(0.001));
            assertThat(findCurrency(result, "EUR").officialRate()).isCloseTo(44.0, within(0.001));
        }
    }
}