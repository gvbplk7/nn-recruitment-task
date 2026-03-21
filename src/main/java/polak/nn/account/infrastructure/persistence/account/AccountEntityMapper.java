package polak.nn.account.infrastructure.persistence.account;

import org.mapstruct.Mapper;
import polak.nn.account.domain.model.Account;

@Mapper(componentModel = "spring")
public interface AccountEntityMapper {
    Account toDomain(AccountEntity entity);

    AccountEntity toEntity(Account account);
}
