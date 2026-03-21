package polak.nn.exchange.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import polak.nn.shared.model.Currency;
import polak.nn.exchange.domain.model.ExchangeRate;
import polak.nn.exchange.domain.port.ExchangeRateRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaExchangeRateRepository implements ExchangeRateRepository {

    private final SpringDataExchangeRateRepository springData;

    @Override
    public void save(ExchangeRate exchangeRate) {
        ExchangeRateEntity entity = springData.findByCurrency(exchangeRate.getCurrency())
                .orElseGet(ExchangeRateEntity::new);
        entity.setCurrency(exchangeRate.getCurrency());
        entity.setRate(exchangeRate.getRate());
        entity.setFetchedAt(exchangeRate.getFetchedAt());
        springData.save(entity);
    }

    @Override
    public Optional<ExchangeRate> findByCurrency(Currency currency) {
        return springData.findByCurrency(currency).map(this::toDomain);
    }

    @Override
    public List<ExchangeRate> findAll() {
        return springData.findAll().stream().map(this::toDomain).toList();
    }

    private ExchangeRate toDomain(ExchangeRateEntity entity) {
        return new ExchangeRate(entity.getCurrency(), entity.getRate(), entity.getFetchedAt());
    }
}
