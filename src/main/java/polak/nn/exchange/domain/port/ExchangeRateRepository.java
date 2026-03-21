package polak.nn.exchange.domain.port;

import polak.nn.shared.model.Currency;
import polak.nn.exchange.domain.model.ExchangeRate;

import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository {

    void save(ExchangeRate exchangeRate);

    Optional<ExchangeRate> findByCurrency(Currency currency);

    List<ExchangeRate> findAll();
}
