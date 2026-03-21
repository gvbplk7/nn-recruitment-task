package polak.nn.account.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import polak.nn.account.domain.exception.AccountNotFoundException;
import polak.nn.account.domain.model.Account;
import polak.nn.account.domain.port.AccountRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAccountUseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private GetAccountUseCase getAccountUseCase;

    @Test
    void shouldReturnAccountWhenFound() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account();
        account.setId(accountId);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        Account result = getAccountUseCase.execute(accountId);

        assertThat(result.getId()).isEqualTo(accountId);
    }

    @Test
    void shouldThrowWhenAccountNotFound() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getAccountUseCase.execute(accountId))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
