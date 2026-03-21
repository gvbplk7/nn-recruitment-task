package polak.nn.account.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import polak.nn.account.domain.model.Currency;

import java.math.BigDecimal;

public record ExchangeCurrencyRequest(
        @NotNull(message = "Source currency is required") Currency from,

        @NotNull(message = "Target currency is required") Currency to,

        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount) {
}
