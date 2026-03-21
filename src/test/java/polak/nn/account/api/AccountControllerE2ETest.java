package polak.nn.account.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import polak.nn.shared.model.Currency;
import polak.nn.exchange.infrastructure.nbp.NbpApiClient;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private void stubNbpRate() {
        when(nbpApiClient.fetchRate(Currency.USD)).thenReturn(new BigDecimal("4.0"));
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
        MvcResult createResult = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "firstName": "Jan",
                            "lastName": "Kowalski",
                            "initialBalancePln": 500.00
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        String accountId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("accountId").asText();

        mockMvc.perform(get("/api/accounts/" + accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId))
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.lastName").value("Kowalski"))
                .andExpect(jsonPath("$.balances.PLN").value(500.00));
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
        MvcResult createResult = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "firstName": "Jan",
                            "lastName": "Kowalski",
                            "initialBalancePln": 1000.00
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        String accountId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("accountId").asText();

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
                .andExpect(jsonPath("$.balances.PLN").value(600.00))
                .andExpect(jsonPath("$.balances.USD").value(100.00));
    }

    @Test
    void shouldReturn400WhenInsufficientBalance() throws Exception {
        stubNbpRate();
        MvcResult createResult = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "firstName": "Jan",
                            "lastName": "Kowalski",
                            "initialBalancePln": 100.00
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        String accountId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("accountId").asText();

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
        MvcResult createResult = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "firstName": "Jan",
                            "lastName": "Kowalski",
                            "initialBalancePln": 1000.00
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        String accountId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("accountId").asText();

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
        MvcResult createResult = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "firstName": "Jan",
                            "lastName": "Kowalski",
                            "initialBalancePln": 1000.00
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        String accountId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("accountId").asText();

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
        MvcResult createResult = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "firstName": "Anna",
                            "lastName": "Nowak",
                            "initialBalancePln": 2000.00
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        String accountId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("accountId").asText();

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
                .andExpect(jsonPath("$.balances.PLN").value(1200.00))
                .andExpect(jsonPath("$.balances.USD").value(200.00));
    }

    @Test
    void shouldExchangeBackAndForth() throws Exception {
        stubNbpRate();
        MvcResult createResult = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "firstName": "Piotr",
                            "lastName": "Wisniewski",
                            "initialBalancePln": 1000.00
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        String accountId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("accountId").asText();

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
                .andExpect(jsonPath("$.balances.PLN").value(600.00))
                .andExpect(jsonPath("$.balances.USD").value(100.00));

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
                .andExpect(jsonPath("$.balances.PLN").value(800.00))
                .andExpect(jsonPath("$.balances.USD").value(50.00));
    }

    @Test
    void shouldReturn400WhenAmountIsNegative() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "firstName": "Jan",
                            "lastName": "Kowalski",
                            "initialBalancePln": 1000.00
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        String accountId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("accountId").asText();

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
        MvcResult createResult = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "firstName": "Jan",
                            "lastName": "Kowalski",
                            "initialBalancePln": 1000.00
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        String accountId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("accountId").asText();

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
}
