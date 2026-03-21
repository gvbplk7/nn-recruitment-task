package polak.nn.account.infrastructure.mapper;

import org.mapstruct.Mapper;
import polak.nn.account.domain.model.Account;
import polak.nn.account.infrastructure.persistence.account.AccountEntity;

@Mapper(componentModel = "spring")
public interface AccountEntityMapper {
    Account toDomain(AccountEntity entity);

    AccountEntity toEntity(Account account);
}
