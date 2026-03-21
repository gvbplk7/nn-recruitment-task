package polak.nn.account.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import polak.nn.account.domain.exception.AccountNotFoundException;
import polak.nn.account.domain.model.Account;
import polak.nn.account.domain.port.AccountRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetAccountUseCase {

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public Account execute(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }
}
