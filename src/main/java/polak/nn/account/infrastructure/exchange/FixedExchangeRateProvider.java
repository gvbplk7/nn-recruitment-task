package polak.nn.account.infrastructure.exchange;

import org.springframework.stereotype.Component;
import polak.nn.account.domain.model.Currency;
import polak.nn.account.domain.port.ExchangeRateProvider;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class FixedExchangeRateProvider implements ExchangeRateProvider {

    private static final Map<String, BigDecimal> RATES = Map.of(
            key(Currency.PLN, Currency.USD), new BigDecimal("0.25"),
            key(Currency.USD, Currency.PLN), new BigDecimal("4.0"));

    @Override
    public BigDecimal getRate(Currency from, Currency to) {
        BigDecimal rate = RATES.get(key(from, to));
        if (rate == null) {
            throw new IllegalArgumentException("No exchange rate available for " + from + " -> " + to);
        }
        return rate;
    }

    private static String key(Currency from, Currency to) {
        return from.name() + "_" + to.name();
    }
}
