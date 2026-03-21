package polak.nn.account.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import polak.nn.account.domain.model.AccountBalanceHistory;
import polak.nn.account.domain.port.AccountBalanceHistoryRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetAccountHistoryUseCase {

    private final AccountBalanceHistoryRepository historyRepository;

    @Transactional(readOnly = true)
    public List<AccountBalanceHistory> execute(UUID userId) {
        return historyRepository.findByUserId(userId);
    }
}
