package polak.nn.account.domain.port;

import polak.nn.shared.model.Currency;

import java.math.BigDecimal;

public interface ExchangeRateProvider {
    BigDecimal getRate(Currency from, Currency to);
}
