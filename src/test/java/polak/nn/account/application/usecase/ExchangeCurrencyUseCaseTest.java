package polak.nn.account.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import polak.nn.account.application.event.BalanceChangedEvent;
import polak.nn.account.domain.exception.AccountNotFoundException;
import polak.nn.account.domain.exception.InsufficientBalanceException;
import polak.nn.account.domain.exception.SameCurrencyExchangeException;
import polak.nn.account.domain.model.Account;
import polak.nn.shared.model.Currency;
import polak.nn.account.domain.port.AccountRepository;
import polak.nn.account.domain.port.ExchangeRateProvider;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeCurrencyUseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ExchangeRateProvider exchangeRateProvider;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ExchangeCurrencyUseCase exchangeCurrencyUseCase;

    @Test
    void shouldExchangeSuccessfully() {
        UUID accountId = UUID.randomUUID();
        Account account = createAccount(accountId, "1000.00");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(exchangeRateProvider.getRate(Currency.PLN, Currency.USD)).thenReturn(new BigDecimal("0.25"));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Account result = exchangeCurrencyUseCase.execute(accountId, Currency.PLN, Currency.USD,
                new BigDecimal("100.00"));

        assertThat(result.getBalances().get(Currency.PLN)).isEqualByComparingTo("900.00");
        assertThat(result.getBalances().get(Currency.USD)).isEqualByComparingTo("25.00");
    }

    @Test
    void shouldPublishEventOnSuccessfulExchange() {
        UUID accountId = UUID.randomUUID();
        Account account = createAccount(accountId, "1000.00");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(exchangeRateProvider.getRate(Currency.PLN, Currency.USD)).thenReturn(new BigDecimal("0.25"));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        exchangeCurrencyUseCase.execute(accountId, Currency.PLN, Currency.USD, new BigDecimal("100.00"));

        ArgumentCaptor<BalanceChangedEvent> captor = ArgumentCaptor.forClass(BalanceChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        BalanceChangedEvent event = captor.getValue();
        assertThat(event.userId()).isEqualTo(accountId);
        assertThat(event.changes()).hasSize(2);
    }

    @Test
    void shouldThrowWhenAccountNotFound() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exchangeCurrencyUseCase.execute(
                accountId, Currency.PLN, Currency.USD, new BigDecimal("100.00")))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void shouldThrowWhenInsufficientBalance() {
        UUID accountId = UUID.randomUUID();
        Account account = createAccount(accountId, "50.00");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(exchangeRateProvider.getRate(Currency.PLN, Currency.USD)).thenReturn(new BigDecimal("0.25"));

        assertThatThrownBy(() -> exchangeCurrencyUseCase.execute(
                accountId, Currency.PLN, Currency.USD, new BigDecimal("100.00")))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void shouldThrowWhenSameCurrency() {
        UUID accountId = UUID.randomUUID();

        assertThatThrownBy(() -> exchangeCurrencyUseCase.execute(
                accountId, Currency.PLN, Currency.PLN, new BigDecimal("100.00")))
                .isInstanceOf(SameCurrencyExchangeException.class);
    }

    @Test
    void shouldNotPublishEventWhenExchangeFails() {
        UUID accountId = UUID.randomUUID();
        Account account = createAccount(accountId, "50.00");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(exchangeRateProvider.getRate(Currency.PLN, Currency.USD)).thenReturn(new BigDecimal("0.25"));

        try {
            exchangeCurrencyUseCase.execute(accountId, Currency.PLN, Currency.USD, new BigDecimal("100.00"));
        } catch (InsufficientBalanceException ignored) {
        }

        verify(eventPublisher, never()).publishEvent(any());
    }

    private Account createAccount(UUID id, String plnBalance) {
        Account account = new Account();
        account.setId(id);
        account.setFirstName("Jan");
        account.setLastName("Kowalski");
        account.setBalances(new EnumMap<>(Currency.class));
        account.getBalances().put(Currency.PLN, new BigDecimal(plnBalance));
        return account;
    }
}
