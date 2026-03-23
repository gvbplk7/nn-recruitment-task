package polak.nn.account.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import polak.nn.shared.model.Currency;

import java.math.BigDecimal;

public record ExchangeCurrencyRequest(
        @NotNull(message = "Source currency is required") Currency from,

        @NotNull(message = "Target currency is required") Currency to,

        @NotNull(message = "Amount is required") @DecimalMin(value = "1.00", message = "Minimum exchange amount is 1.00") BigDecimal amount) {
}
