package polak.nn.account.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import polak.nn.account.domain.exception.InsufficientBalanceException;
import polak.nn.account.domain.exception.SameCurrencyExchangeException;

@Getter
@Setter
@NoArgsConstructor
public class Account {
    private UUID id;
    private String firstName;
    private String lastName;
    private Map<Currency, BigDecimal> balances = new HashMap<>();
    private Long version;

    public List<BalanceChange> exchange(Currency from, Currency to, BigDecimal amount, BigDecimal rate) {
        if (from == to) {
            throw new SameCurrencyExchangeException(from);
        }

        BigDecimal currentFromBalance = balances.getOrDefault(from, BigDecimal.ZERO);
        if (currentFromBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(from, amount, currentFromBalance);
        }

        BigDecimal exchangedAmount = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal previousFromBalance = currentFromBalance;
        BigDecimal previousToBalance = balances.getOrDefault(to, BigDecimal.ZERO);

        balances.put(from, currentFromBalance.subtract(amount));
        balances.put(to, previousToBalance.add(exchangedAmount));

        Instant now = Instant.now();
        return List.of(
                new BalanceChange(from, previousFromBalance, balances.get(from), now),
                new BalanceChange(to, previousToBalance, balances.get(to), now));
    }
}