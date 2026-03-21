package polak.nn.account.application.event;

import polak.nn.account.domain.model.BalanceChange;

import java.util.List;
import java.util.UUID;

public record BalanceChangedEvent(UUID userId, List<BalanceChange> changes) {
}
