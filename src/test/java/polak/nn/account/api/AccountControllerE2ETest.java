package polak.nn.account.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import polak.nn.account.infrastructure.persistence.account.AccountEntity;
import polak.nn.account.infrastructure.persistence.account.SpringDataAccountRepository;
import polak.nn.exchange.domain.exception.ExchangeRateUnavailableException;
import polak.nn.exchange.infrastructure.nbp.NbpApiClient;
import polak.nn.exchange.infrastructure.nbp.NbpExchangeRateProvider;
import polak.nn.shared.model.Currency;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NbpApiClient nbpApiClient;

    @MockitoSpyBean
    private SpringDataAccountRepository springDataAccountRepository;

    @MockitoSpyBean
    private NbpExchangeRateProvider nbpExchangeRateProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private void stubNbpRate() {
        when(nbpApiClient.fetchRate(Currency.USD)).thenReturn(new BigDecimal("4.0"));
    }

    private UUID createAccount(String firstName, String lastName, BigDecimal initialBalancePln) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "firstName": "%s",
                            "lastName": "%s",
                            "initialBalancePln": %s
                        }
                        """.formatted(firstName, lastName, initialBalancePln)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString())
                        .get("accountId").asText());
    }

    @Test
    void shouldCreateAccountAndReturnId() throws Exception {
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "firstName": "Jan",
                            "lastName": "Kowalski",
                            "initialBalancePln": 1000.00
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").isNotEmpty());
    }

    @Test
    void shouldReturnBadRequestWhenFirstNameIsBlank() throws Exception {
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "firstName": "",
                            "lastName": "Kowalski",
                            "initialBalancePln": 1000.00
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void shouldReturnBadRequestWhenLastNameIsMissing() throws Exception {
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "firstName": "Jan",
                            "initialBalancePln": 1000.00
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenBalanceIsNegative() throws Exception {
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "firstName": "Jan",
                            "lastName": "Kowalski",
                            "initialBalancePln": -100.00
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetAccountById() throws Exception {
        UUID accountId = createAccount("Jan", "Kowalski", new BigDecimal("500.00"));

        mockMvc.perform(get("/api/accounts/" + accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId.toString()))
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.lastName").value("Kowalski"))
                .andExpect(jsonPath("$.balances.PLN", comparesEqualTo(500.00)));
    }

    @Test
    void shouldReturn404ForNonExistentAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void shouldExchangePlnToUsd() throws Exception {
        stubNbpRate();
        UUID accountId = createAccount("Jan", "Kowalski", new BigDecimal("1000.00"));

        mockMvc.perform(post("/api/accounts/" + accountId + "/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "from": "PLN",
                            "to": "USD",
                            "amount": 400.00
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balances.PLN", comparesEqualTo(600.00)))
                .andExpect(jsonPath("$.balances.USD", comparesEqualTo(100.00)));
    }

    @Test
    void shouldReturn400WhenInsufficientBalance() throws Exception {
        stubNbpRate();
        UUID accountId = createAccount("Jan", "Kowalski", new BigDecimal("100.00"));

        mockMvc.perform(post("/api/accounts/" + accountId + "/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "from": "PLN",
                            "to": "USD",
                            "amount": 500.00
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Insufficient")));
    }

    @Test
    void shouldReturn400WhenSameCurrencyExchange() throws Exception {
        UUID accountId = createAccount("Jan", "Kowalski", new BigDecimal("1000.00"));

        mockMvc.perform(post("/api/accounts/" + accountId + "/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "from": "PLN",
                            "to": "PLN",
                            "amount": 100.00
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("same currency")));
    }

    @Test
    void shouldReturn400WhenInvalidCurrency() throws Exception {
        UUID accountId = createAccount("Jan", "Kowalski", new BigDecimal("1000.00"));

        mockMvc.perform(post("/api/accounts/" + accountId + "/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "from": "PLN",
                            "to": "FAKE",
                            "amount": 100.00
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldExchangeAndThenVerifyBalanceViaGet() throws Exception {
        stubNbpRate();
        UUID accountId = createAccount("Anna", "Nowak", new BigDecimal("2000.00"));

        mockMvc.perform(post("/api/accounts/" + accountId + "/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "from": "PLN",
                            "to": "USD",
                            "amount": 800.00
                        }
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/accounts/" + accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balances.PLN", comparesEqualTo(1200.00)))
                .andExpect(jsonPath("$.balances.USD", comparesEqualTo(200.00)));
    }

    @Test
    void shouldExchangeBackAndForth() throws Exception {
        stubNbpRate();
        UUID accountId = createAccount("Piotr", "Wisniewski", new BigDecimal("1000.00"));

        // PLN -> USD: 400 PLN * 0.25 = 100 USD
        mockMvc.perform(post("/api/accounts/" + accountId + "/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "from": "PLN",
                            "to": "USD",
                            "amount": 400.00
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balances.PLN", comparesEqualTo(600.00)))
                .andExpect(jsonPath("$.balances.USD", comparesEqualTo(100.00)));

        // USD -> PLN: 50 USD * 4.0 = 200 PLN
        mockMvc.perform(post("/api/accounts/" + accountId + "/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "from": "USD",
                            "to": "PLN",
                            "amount": 50.00
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balances.PLN", comparesEqualTo(800.00)))
                .andExpect(jsonPath("$.balances.USD", comparesEqualTo(50.00)));
    }

    @Test
    void shouldReturn400WhenAmountIsNegative() throws Exception {
        UUID accountId = createAccount("Jan", "Kowalski", new BigDecimal("1000.00"));

        mockMvc.perform(post("/api/accounts/" + accountId + "/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "from": "PLN",
                            "to": "USD",
                            "amount": -100.00
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenAmountIsZero() throws Exception {
        UUID accountId = createAccount("Jan", "Kowalski", new BigDecimal("1000.00"));

        mockMvc.perform(post("/api/accounts/" + accountId + "/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "from": "PLN",
                            "to": "USD",
                            "amount": 0
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400ForExchangeOnNonExistentAccount() throws Exception {
        stubNbpRate();
        mockMvc.perform(post("/api/accounts/00000000-0000-0000-0000-000000000000/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "from": "PLN",
                            "to": "USD",
                            "amount": 100.00
                        }
                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenAccountModifiedConcurrently() throws Exception {
        stubNbpRate();
        UUID accountId = createAccount("Jan", "Kowalski", new BigDecimal("1000.00"));

        doThrow(new ObjectOptimisticLockingFailureException(AccountEntity.class, accountId))
                .when(springDataAccountRepository).save(any());

        mockMvc.perform(post("/api/accounts/" + accountId + "/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "from": "PLN",
                            "to": "USD",
                            "amount": 100.00
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("The account was modified by another transaction. Please refresh and try again."));
    }

    @Test
    void shouldReturn503WhenNbpApiFails() throws Exception {
        UUID accountId = createAccount("Jan", "Kowalski", new BigDecimal("1000.00"));

        doThrow(new ExchangeRateUnavailableException("Exchange rate for USD is unavailable. Please try again later."))
                .when(nbpExchangeRateProvider).getRate(eq(Currency.PLN), eq(Currency.USD));

        mockMvc.perform(post("/api/accounts/" + accountId + "/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "from": "PLN",
                            "to": "USD",
                            "amount": 100.00
                        }
                        """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }
}
