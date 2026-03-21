package polak.nn.exchange.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import polak.nn.shared.model.Currency;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataExchangeRateRepository extends JpaRepository<ExchangeRateEntity, UUID> {
    Optional<ExchangeRateEntity> findByCurrency(Currency currency);
}
