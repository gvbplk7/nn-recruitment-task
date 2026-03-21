package polak.nn.account.infrastructure.persistence.history;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import polak.nn.account.domain.model.Currency;

@Entity
@Table(name = "account_balance_history", indexes = {
        @Index(name = "idx_account_balance_history_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class AccountBalanceHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_currency", nullable = false, length = 3)
    private Currency fromCurrency;

    @Column(name = "from_previous_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal fromPreviousBalance;

    @Column(name = "from_new_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal fromNewBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_currency", nullable = false, length = 3)
    private Currency toCurrency;

    @Column(name = "to_previous_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal toPreviousBalance;

    @Column(name = "to_new_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal toNewBalance;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}
