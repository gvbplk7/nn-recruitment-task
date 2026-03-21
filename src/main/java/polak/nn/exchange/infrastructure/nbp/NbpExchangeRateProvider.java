package polak.nn.exchange.infrastructure.nbp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import polak.nn.shared.model.Currency;
import polak.nn.exchange.domain.exception.ExchangeRateUnavailableException;
import polak.nn.exchange.domain.model.ExchangeRate;
import polak.nn.account.domain.port.ExchangeRateProvider;
import polak.nn.exchange.domain.port.ExchangeRateRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NbpExchangeRateProvider implements ExchangeRateProvider {

    private static final Duration WEEKDAY_REFRESH_INTERVAL = Duration.ofHours(3);
    private static final Duration HARD_STALE_LIMIT = Duration.ofHours(12);
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    private final NbpApiClient nbpApiClient;
    private final ExchangeRateRepository exchangeRateRepository;

    private final ConcurrentMap<Currency, ExchangeRate> cache = new ConcurrentHashMap<>();

    @Override
    public BigDecimal getRate(Currency from, Currency to) {
        if (from == Currency.PLN) {
            return BigDecimal.ONE.divide(getPlnRate(to), 6, RoundingMode.HALF_UP);
        }
        return getPlnRate(from);
    }

    private BigDecimal getPlnRate(Currency currency) {
        if (currency == Currency.PLN) {
            return BigDecimal.ONE;
        }

        ExchangeRate cached = cache.get(currency);
        Instant now = Instant.now();

        if (cached != null && !needsRefresh(cached.getFetchedAt(), now)) {
            return cached.getRate();
        }

        try {
            BigDecimal freshRate = nbpApiClient.fetchRate(currency);
            log.info("Fetched rate for {} from NBP API: {}", currency, freshRate);
            ExchangeRate exchangeRate = new ExchangeRate(currency, freshRate, now);
            exchangeRateRepository.save(exchangeRate);
            cache.put(currency, exchangeRate);
            return freshRate;
        } catch (Exception e) {
            log.warn("Failed to fetch rate from NBP API for {}: {}", currency, e.getMessage());
            return fallbackToCache(currency, cached, now);
        }
    }

    private boolean needsRefresh(Instant fetchedAt, Instant now) {
        if (isWeekend(now)) {
            return false;
        }
        return Duration.between(fetchedAt, now).compareTo(WEEKDAY_REFRESH_INTERVAL) > 0;
    }

    private boolean isWeekend(Instant instant) {
        DayOfWeek day = LocalDate.ofInstant(instant, WARSAW).getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private BigDecimal fallbackToCache(Currency currency, ExchangeRate cached, Instant now) {
        if (cached != null && !isHardStale(cached.getFetchedAt(), now)) {
            log.info("Using cached rate for {} (fetched at {})", currency, cached.getFetchedAt());
            return cached.getRate();
        }
        throw new ExchangeRateUnavailableException(
                "Exchange rate for " + currency + " is unavailable. Please try again later.");
    }

    private boolean isHardStale(Instant fetchedAt, Instant now) {
        return Duration.between(fetchedAt, now).compareTo(HARD_STALE_LIMIT) > 0;
    }

    public void warmUpCache() {
        log.info("Warming up exchange rate cache from database...");
        exchangeRateRepository.findAll().forEach(exchangeRate -> {
            cache.put(exchangeRate.getCurrency(), exchangeRate);
            log.info("Loaded rate for {}: {} (fetched at {})",
                    exchangeRate.getCurrency(), exchangeRate.getRate(), exchangeRate.getFetchedAt());
        });
    }
}
