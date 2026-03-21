package polak.nn.exchange.infrastructure.nbp;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import polak.nn.shared.model.Currency;

import java.math.BigDecimal;

@Slf4j
@Component
public class NbpApiClient {

    private final RestClient restClient;

    public NbpApiClient(@Value("${nbp.api.base-url}") String baseUrl, RestClient.Builder builder) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @CircuitBreaker(name = "nbpApi")
    public BigDecimal fetchRate(Currency currency) {
        log.info("Fetching exchange rate from NBP API for {}", currency);
        NbpApiResponse response = restClient.get()
                .uri("/api/exchangerates/rates/a/{currency}/?format=json", currency.name())
                .retrieve()
                .body(NbpApiResponse.class);

        if (response == null || response.rates() == null || response.rates().isEmpty()) {
            throw new RuntimeException("Empty response from NBP API for " + currency);
        }

        return response.rates().getFirst().mid();
    }
}
