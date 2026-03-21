package polak.nn.account.domain.exception;

import polak.nn.shared.model.Currency;

public class UnsupportedCurrencyPairException extends RuntimeException {
    public UnsupportedCurrencyPairException(Currency from, Currency to) {
        super("Unsupported currency pair: " + from + " → " + to + ". One side must be PLN.");
    }
}
