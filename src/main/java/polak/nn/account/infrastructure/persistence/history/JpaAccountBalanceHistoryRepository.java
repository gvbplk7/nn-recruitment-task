package polak.nn.account.infrastructure.persistence.history;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import polak.nn.account.domain.model.AccountBalanceHistory;
import polak.nn.account.domain.port.AccountBalanceHistoryRepository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaAccountBalanceHistoryRepository implements AccountBalanceHistoryRepository {

    private final SpringDataAccountBalanceHistoryRepository springDataRepository;
    private final AccountBalanceHistoryEntityMapper mapper;

    @Override
    public void save(AccountBalanceHistory history) {
        AccountBalanceHistoryEntity entity = mapper.toEntity(history);
        springDataRepository.save(entity);
    }

    @Override
    public List<AccountBalanceHistory> findByUserId(UUID userId) {
        return springDataRepository.findByUserIdOrderByChangedAtDesc(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
