package polak.nn.account.api.dto;

import polak.nn.account.domain.model.Currency;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String firstName,
        String lastName,
        Map<Currency, BigDecimal> balances) {
}
