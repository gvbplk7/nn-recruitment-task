package polak.nn.account.infrastructure.persistence.history;

import org.mapstruct.Mapper;
import polak.nn.account.domain.model.AccountBalanceHistory;

@Mapper(componentModel = "spring")
public interface AccountBalanceHistoryEntityMapper {
    AccountBalanceHistoryEntity toEntity(AccountBalanceHistory domain);

    AccountBalanceHistory toDomain(AccountBalanceHistoryEntity entity);
}
