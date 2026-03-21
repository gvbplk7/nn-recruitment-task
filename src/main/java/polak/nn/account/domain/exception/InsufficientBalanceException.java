package polak.nn.account.domain.exception;

import polak.nn.shared.model.Currency;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(Currency currency, BigDecimal requested, BigDecimal available) {
        super("Insufficient " + currency + " balance. Requested: " + requested + ", available: " + available);
    }
}
