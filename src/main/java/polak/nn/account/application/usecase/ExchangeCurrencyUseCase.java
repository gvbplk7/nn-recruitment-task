package polak.nn.account.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import polak.nn.account.application.event.BalanceChangedEvent;
import polak.nn.account.domain.exception.AccountNotFoundException;
import polak.nn.account.domain.exception.SameCurrencyExchangeException;
import polak.nn.account.domain.model.Account;
import polak.nn.account.domain.model.BalanceChange;
import polak.nn.account.domain.model.Currency;
import polak.nn.account.domain.port.AccountRepository;
import polak.nn.account.domain.port.ExchangeRateProvider;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExchangeCurrencyUseCase {

    private final AccountRepository accountRepository;
    private final ExchangeRateProvider exchangeRateProvider;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Account execute(UUID accountId, Currency from, Currency to, BigDecimal amount) {
        if (from == to) {
            throw new SameCurrencyExchangeException(from);
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        BigDecimal rate = exchangeRateProvider.getRate(from, to);
        List<BalanceChange> changes = account.exchange(from, to, amount, rate);

        Account saved = accountRepository.save(account);
        eventPublisher.publishEvent(new BalanceChangedEvent(accountId, changes));

        return saved;
    }
}
