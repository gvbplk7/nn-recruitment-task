package polak.nn.exchange.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import polak.nn.shared.model.Currency;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRate {

    private Currency currency;

    /**
     * Mid rate: how many PLN equals 1 unit of {@code currency}.
     * Sourced from NBP Table A. Used as the pivot for all cross-currency
     * conversions.
     * To add a new currency, simply add it to {@link Currency} — the rest follows
     * automatically.
     */
    private BigDecimal rate;

    private Instant fetchedAt;
}
