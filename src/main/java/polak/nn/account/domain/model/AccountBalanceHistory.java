package polak.nn.account.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccountBalanceHistory {
    private UUID id;
    private UUID userId;
    private Currency fromCurrency;
    private BigDecimal fromPreviousBalance;
    private BigDecimal fromNewBalance;
    private Currency toCurrency;
    private BigDecimal toPreviousBalance;
    private BigDecimal toNewBalance;
    private Instant changedAt;
}
