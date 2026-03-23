package polak.nn.account.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import polak.nn.account.api.dto.ErrorResponse;
import polak.nn.account.api.dto.ExchangeCurrencyRequest;
import polak.nn.account.application.usecase.CreateAccountUseCase;
import polak.nn.account.application.usecase.ExchangeCurrencyUseCase;
import polak.nn.account.application.usecase.GetAccountHistoryUseCase;
import polak.nn.account.application.usecase.GetAccountUseCase;
import polak.nn.account.domain.model.Account;
import polak.nn.account.domain.model.AccountBalanceHistory;

import java.util.List;
import java.util.UUID;

@Tag(name = "Accounts", description = "Zarządzanie kontami walutowymi i wymiana PLN ↔ USD")
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final ExchangeCurrencyUseCase exchangeCurrencyUseCase;
    private final GetAccountUseCase getAccountUseCase;
    private final GetAccountHistoryUseCase getAccountHistoryUseCase;

    @Operation(summary = "Utwórz nowe konto", description = "Zakłada nowe konto walutowe z podanym imieniem, nazwiskiem i początkowym saldem w PLN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Konto zostało pomyślnie utworzone"),
            @ApiResponse(responseCode = "400", description = "Błąd walidacji danych wejściowych", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = """
                    {
                      "status": 400,
                      "message": "firstName: First name is required",
                      "timestamp": "2026-03-23T12:00:00Z"
                    }""")))
    })
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

    @Operation(summary = "Wymień waluty", description = "Wykonuje wymianę walut na koncie. Przynajmniej jedna z walut musi być PLN. Kurs pobierany jest z API NBP i cache'owany przez 3h w dni robocze.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wymiana wykonana pomyślnie — zwraca zaktualizowany stan konta"),
            @ApiResponse(responseCode = "400", description = "Niewystarczające saldo, ta sama waluta lub nieobsługiwana para walutowa", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                    @ExampleObject(name = "Niewystarczające saldo", value = """
                            {
                              "status": 400,
                              "message": "Insufficient balance: required 500.00 PLN, available 100.00 PLN",
                              "timestamp": "2026-03-23T12:00:00Z"
                            }"""),
                    @ExampleObject(name = "Ta sama waluta", value = """
                            {
                              "status": 400,
                              "message": "Cannot exchange same currency: PLN",
                              "timestamp": "2026-03-23T12:00:00Z"
                            }"""),
                    @ExampleObject(name = "Nieobsługiwana para", value = """
                            {
                              "status": 400,
                              "message": "Unsupported currency pair: USD → EUR. One side must be PLN.",
                              "timestamp": "2026-03-23T12:00:00Z"
                            }"""),
                    @ExampleObject(name = "Kwota poniżej minimum", value = """
                            {
                              "status": 400,
                              "message": "amount: Minimum exchange amount is 1.00",
                              "timestamp": "2026-03-23T12:00:00Z"
                            }""")
            })),
            @ApiResponse(responseCode = "404", description = "Konto o podanym ID nie istnieje", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = """
                    {
                      "status": 404,
                      "message": "Account not found: 00000000-0000-0000-0000-000000000000",
                      "timestamp": "2026-03-23T12:00:00Z"
                    }"""))),
            @ApiResponse(responseCode = "409", description = "Konflikt wersji — konto było modyfikowane w innym żądaniu. Spróbuj ponownie.", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = """
                    {
                      "status": 409,
                      "message": "The account was modified by another transaction. Please refresh and try again.",
                      "timestamp": "2026-03-23T12:00:00Z"
                    }"""))),
            @ApiResponse(responseCode = "503", description = "Kurs walutowy niedostępny — API NBP nie odpowiada i brak świeżego kursu w cache", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = """
                    {
                      "status": 503,
                      "message": "Exchange rate for USD is unavailable. Please try again later.",
                      "timestamp": "2026-03-23T12:00:00Z"
                    }""")))

    })
    @PostMapping("/{id}/exchange")
    public ResponseEntity<AccountResponse> exchange(
            @Parameter(description = "UUID konta") @PathVariable UUID id,
            @Valid @RequestBody ExchangeCurrencyRequest request) {
        Account account = exchangeCurrencyUseCase.execute(
                id,
                request.from(),
                request.to(),
                request.amount());
        return ResponseEntity.ok(toResponse(account));
    }

    @Operation(summary = "Pobierz dane konta", description = "Zwraca dane konta wraz z aktualnymi saldami we wszystkich walutach.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dane konta"),
            @ApiResponse(responseCode = "404", description = "Konto o podanym ID nie istnieje", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = """
                    {
                      "status": 404,
                      "message": "Account not found: 00000000-0000-0000-0000-000000000000",
                      "timestamp": "2026-03-23T12:00:00Z"
                    }""")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(
            @Parameter(description = "UUID konta") @PathVariable UUID id) {
        Account account = getAccountUseCase.execute(id);
        return ResponseEntity.ok(toResponse(account));
    }

    @Operation(summary = "Historia wymian", description = "Zwraca chronologiczną historię wszystkich operacji wymiany walut na koncie, wraz z kursem zastosowanym przy każdej operacji.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista operacji wymiany walut"),
            @ApiResponse(responseCode = "404", description = "Konto o podanym ID nie istnieje", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = """
                    {
                      "status": 404,
                      "message": "Account not found: 00000000-0000-0000-0000-000000000000",
                      "timestamp": "2026-03-23T12:00:00Z"
                    }""")))
    })
    @GetMapping("/{id}/history")
    public ResponseEntity<List<AccountBalanceHistoryResponse>> getHistory(
            @Parameter(description = "UUID konta") @PathVariable UUID id) {
        // we could use pagination here if we expect a lot of history records
        // but for simplicity we return all records in one response
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
