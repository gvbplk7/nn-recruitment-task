package polak.nn.account.infrastructure.mapper;

import org.mapstruct.Mapper;
import polak.nn.account.domain.model.AccountBalanceHistory;
import polak.nn.account.infrastructure.persistence.history.AccountBalanceHistoryEntity;

@Mapper(componentModel = "spring")
public interface AccountBalanceHistoryEntityMapper {
    AccountBalanceHistoryEntity toEntity(AccountBalanceHistory domain);

    AccountBalanceHistory toDomain(AccountBalanceHistoryEntity entity);
}
