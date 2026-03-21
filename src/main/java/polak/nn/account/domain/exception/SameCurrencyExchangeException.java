package polak.nn.account.domain.exception;

import polak.nn.shared.model.Currency;

public class SameCurrencyExchangeException extends RuntimeException {
    public SameCurrencyExchangeException(Currency currency) {
        super("Cannot exchange " + currency + " to the same currency");
    }
}
