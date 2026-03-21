package polak.nn.account.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import polak.nn.account.api.dto.AccountBalanceHistoryResponse;
import polak.nn.account.api.dto.AccountResponse;
import polak.nn.account.api.dto.CreateAccountRequest;
import polak.nn.account.api.dto.CreateAccountResponse;
import polak.nn.account.api.dto.ExchangeCurrencyRequest;
import polak.nn.account.application.usecase.CreateAccountUseCase;
import polak.nn.account.application.usecase.ExchangeCurrencyUseCase;
import polak.nn.account.application.usecase.GetAccountHistoryUseCase;
import polak.nn.account.application.usecase.GetAccountUseCase;
import polak.nn.account.domain.model.Account;
import polak.nn.account.domain.model.AccountBalanceHistory;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final ExchangeCurrencyUseCase exchangeCurrencyUseCase;
    private final GetAccountUseCase getAccountUseCase;
    private final GetAccountHistoryUseCase getAccountHistoryUseCase;

    @PostMapping
    public ResponseEntity<CreateAccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Account account = createAccountUseCase.execute(
                request.firstName(),
                request.lastName(),
                request.initialBalancePln());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new CreateAccountResponse(account.getId()));
    }

    @PostMapping("/{id}/exchange")
    public ResponseEntity<AccountResponse> exchange(
            @PathVariable UUID id,
            @Valid @RequestBody ExchangeCurrencyRequest request) {
        Account account = exchangeCurrencyUseCase.execute(
                id,
                request.from(),
                request.to(),
                request.amount());
        return ResponseEntity.ok(toResponse(account));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        Account account = getAccountUseCase.execute(id);
        return ResponseEntity.ok(toResponse(account));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<AccountBalanceHistoryResponse>> getHistory(@PathVariable UUID id) {
        List<AccountBalanceHistoryResponse> history = getAccountHistoryUseCase.execute(id).stream()
                .map(this::toHistoryResponse)
                .toList();
        return ResponseEntity.ok(history);
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getFirstName(),
                account.getLastName(),
                account.getBalances());
    }

    private AccountBalanceHistoryResponse toHistoryResponse(AccountBalanceHistory h) {
        return new AccountBalanceHistoryResponse(
                h.getId(),
                h.getUserId(),
                h.getFromCurrency(),
                h.getFromPreviousBalance(),
                h.getFromNewBalance(),
                h.getToCurrency(),
                h.getToPreviousBalance(),
                h.getToNewBalance(),
                h.getRate(),
                h.getChangedAt());
    }
}
