package polak.nn.account.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import polak.nn.account.domain.model.Account;
import polak.nn.shared.model.Currency;
import polak.nn.account.domain.port.AccountRepository;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAccountUseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private CreateAccountUseCase createAccountUseCase;

    @Test
    void shouldCreateAccountWithInitialPlnBalance() {
        Account savedAccount = new Account();
        savedAccount.setId(UUID.randomUUID());
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        Account result = createAccountUseCase.execute("Jan", "Kowalski", new BigDecimal("1000.00"));

        assertThat(result.getId()).isNotNull();
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void shouldSetFirstNameAndLastName() {
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = createAccountUseCase.execute("Jan", "Kowalski", new BigDecimal("500.00"));

        assertThat(result.getFirstName()).isEqualTo("Jan");
        assertThat(result.getLastName()).isEqualTo("Kowalski");
    }

    @Test
    void shouldSetPlnBalance() {
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = createAccountUseCase.execute("Jan", "Kowalski", new BigDecimal("1500.00"));

        assertThat(result.getBalances().get(Currency.PLN)).isEqualByComparingTo("1500.00");
    }
}
