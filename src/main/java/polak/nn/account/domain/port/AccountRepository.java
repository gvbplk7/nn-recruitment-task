package polak.nn.account.domain.port;

import polak.nn.account.domain.model.Account;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Account save(Account account);

    Optional<Account> findById(UUID id);
}
