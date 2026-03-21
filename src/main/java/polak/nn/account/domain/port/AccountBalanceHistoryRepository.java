package polak.nn.account.domain.port;

import polak.nn.account.domain.model.AccountBalanceHistory;

import java.util.List;
import java.util.UUID;

public interface AccountBalanceHistoryRepository {
    void save(AccountBalanceHistory history);

    List<AccountBalanceHistory> findByUserId(UUID userId);
}
