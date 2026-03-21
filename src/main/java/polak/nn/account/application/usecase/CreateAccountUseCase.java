package polak.nn.account.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import polak.nn.account.domain.model.Account;
import polak.nn.shared.model.Currency;
import polak.nn.account.domain.port.AccountRepository;

import java.math.BigDecimal;
import java.util.EnumMap;

@Service
@RequiredArgsConstructor
public class CreateAccountUseCase {

    private final AccountRepository accountRepository;

    @Transactional
    public Account execute(String firstName, String lastName, BigDecimal initialBalancePln) {
        Account account = new Account();
        account.setFirstName(firstName);
        account.setLastName(lastName);
        account.setBalances(new EnumMap<>(Currency.class));
        account.getBalances().put(Currency.PLN, initialBalancePln);
        return accountRepository.save(account);
    }
}
