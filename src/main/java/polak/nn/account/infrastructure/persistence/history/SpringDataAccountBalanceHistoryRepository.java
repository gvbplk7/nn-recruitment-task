package polak.nn.account.infrastructure.persistence.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataAccountBalanceHistoryRepository extends JpaRepository<AccountBalanceHistoryEntity, UUID> {
    List<AccountBalanceHistoryEntity> findByUserIdOrderByChangedAtDesc(UUID userId);
}
