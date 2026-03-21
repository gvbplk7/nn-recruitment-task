package polak.nn.account.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BalanceChange {
    private Currency currency;
    private BigDecimal previousBalance;
    private BigDecimal newBalance;
    private Instant changedAt;
}
