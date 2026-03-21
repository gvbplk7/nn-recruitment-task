package polak.nn.account.api.dto;

import polak.nn.account.domain.model.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountBalanceHistoryResponse(
        UUID id,
        UUID userId,
        Currency fromCurrency,
        BigDecimal fromPreviousBalance,
        BigDecimal fromNewBalance,
        Currency toCurrency,
        BigDecimal toPreviousBalance,
        BigDecimal toNewBalance,
        Instant changedAt) {
}
