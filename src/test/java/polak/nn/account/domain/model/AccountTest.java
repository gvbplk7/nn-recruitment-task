package polak.nn.account.domain.model;

import org.junit.jupiter.api.Test;
import polak.nn.account.domain.exception.InsufficientBalanceException;
import polak.nn.account.domain.exception.SameCurrencyExchangeException;
import polak.nn.shared.model.Currency;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    void shouldExchangePlnToUsd() {
        Account account = createAccountWithPln("1000.00");

        List<BalanceChange> changes = account.exchange(
                Currency.PLN, Currency.USD, new BigDecimal("100.00"), new BigDecimal("0.25"));

        assertThat(account.getBalances().get(Currency.PLN)).isEqualByComparingTo("900.00");
        assertThat(account.getBalances().get(Currency.USD)).isEqualByComparingTo("25.00");
        assertThat(changes).hasSize(2);
    }

    @Test
    void shouldExchangeUsdToPln() {
        Account account = createAccountWithPlnAndUsd("1000.00", "100.00");

        List<BalanceChange> changes = account.exchange(
                Currency.USD, Currency.PLN, new BigDecimal("50.00"), new BigDecimal("4.0"));

        assertThat(account.getBalances().get(Currency.USD)).isEqualByComparingTo("50.00");
        assertThat(account.getBalances().get(Currency.PLN)).isEqualByComparingTo("1200.00");
        assertThat(changes).hasSize(2);
    }

    @Test
    void shouldThrowWhenInsufficientBalance() {
        Account account = createAccountWithPln("50.00");

        assertThatThrownBy(() -> account.exchange(
                Currency.PLN, Currency.USD, new BigDecimal("100.00"), new BigDecimal("0.25")))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void shouldThrowWhenExchangingSameCurrency() {
        Account account = createAccountWithPln("1000.00");

        assertThatThrownBy(() -> account.exchange(
                Currency.PLN, Currency.PLN, new BigDecimal("100.00"), new BigDecimal("1.0")))
                .isInstanceOf(SameCurrencyExchangeException.class);
    }

    @Test
    void shouldExchangeFullBalance() {
        Account account = createAccountWithPln("100.00");

        account.exchange(Currency.PLN, Currency.USD, new BigDecimal("100.00"), new BigDecimal("0.25"));

        assertThat(account.getBalances().get(Currency.PLN)).isEqualByComparingTo("0.00");
        assertThat(account.getBalances().get(Currency.USD)).isEqualByComparingTo("25.00");
    }

    @Test
    void shouldReturnCorrectBalanceChanges() {
        Account account = createAccountWithPln("500.00");

        List<BalanceChange> changes = account.exchange(
                Currency.PLN, Currency.USD, new BigDecimal("200.00"), new BigDecimal("0.25"));

        BalanceChange plnChange = changes.stream()
                .filter(c -> c.getCurrency() == Currency.PLN).findFirst().orElseThrow();
        assertThat(plnChange.getPreviousBalance()).isEqualByComparingTo("500.00");
        assertThat(plnChange.getNewBalance()).isEqualByComparingTo("300.00");
        assertThat(plnChange.getChangedAt()).isNotNull();

        BalanceChange usdChange = changes.stream()
                .filter(c -> c.getCurrency() == Currency.USD).findFirst().orElseThrow();
        assertThat(usdChange.getPreviousBalance()).isEqualByComparingTo("0.00");
        assertThat(usdChange.getNewBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    void shouldThrowWhenExchangingFromCurrencyWithZeroBalance() {
        Account account = createAccountWithPln("1000.00");

        assertThatThrownBy(() -> account.exchange(
                Currency.USD, Currency.PLN, new BigDecimal("10.00"), new BigDecimal("4.0")))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    private Account createAccountWithPln(String amount) {
        Account account = new Account();
        account.setBalances(new EnumMap<>(Currency.class));
        account.getBalances().put(Currency.PLN, new BigDecimal(amount));
        return account;
    }

    private Account createAccountWithPlnAndUsd(String pln, String usd) {
        Account account = createAccountWithPln(pln);
        account.getBalances().put(Currency.USD, new BigDecimal(usd));
        return account;
    }
}
