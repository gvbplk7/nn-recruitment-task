package polak.nn.account.infrastructure.persistence.account;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import polak.nn.account.domain.model.Account;
import polak.nn.account.domain.port.AccountRepository;
import polak.nn.account.infrastructure.mapper.AccountEntityMapper;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaAccountRepository implements AccountRepository {

    private final SpringDataAccountRepository springDataRepository;
    private final AccountEntityMapper mapper;

    @Override
    public Account save(Account account) {
        AccountEntity entity = mapper.toEntity(account);
        AccountEntity saved = springDataRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }
}
