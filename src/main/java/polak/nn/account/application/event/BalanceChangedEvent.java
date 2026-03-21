package polak.nn.account.application.event;

import polak.nn.account.domain.model.BalanceChange;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BalanceChangedEvent(UUID userId, List<BalanceChange> changes, BigDecimal rate) {
}
