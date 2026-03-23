package polak.nn.exchange.infrastructure.nbp;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import polak.nn.exchange.domain.exception.ExchangeRateUnavailableException;
import polak.nn.shared.model.Currency;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
public class NbpApiClient {

    private final RestClient restClient;

    public NbpApiClient(
            @Value("${nbp.api.base-url}") String baseUrl,
            @Value("${nbp.api.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${nbp.api.read-timeout-ms:5000}") int readTimeoutMs,
            RestClient.Builder builder) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    @CircuitBreaker(name = "nbpApi", fallbackMethod = "fetchRateFallback")
    public BigDecimal fetchRate(Currency currency) {
        log.info("Fetching exchange rate from NBP API for {}", currency);
        NbpApiResponse response = restClient.get()
                .uri("/api/exchangerates/rates/a/{currency}/?format=json", currency.name())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ExchangeRateUnavailableException(
                            "NBP API returned " + res.getStatusCode().value() + " for " + currency
                                    + ". Rate may not yet be published for today.");
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new ExchangeRateUnavailableException(
                            "NBP API server error " + res.getStatusCode().value() + " for " + currency + ".");
                })
                .body(NbpApiResponse.class);

        return Optional.ofNullable(response)
                .map(NbpApiResponse::rates)
                .filter(rates -> !rates.isEmpty())
                .map(rates -> rates.getFirst().mid())
                .orElseThrow(() -> new ExchangeRateUnavailableException(
                        "Empty response from NBP API for " + currency));
    }

    private BigDecimal fetchRateFallback(Currency currency, CallNotPermittedException ex) {
        log.warn("Circuit breaker open for NBP API — call rejected for {}", currency);
        throw new ExchangeRateUnavailableException(
                "Exchange rate for " + currency + " is unavailable. Please try again later.");
    }

    private BigDecimal fetchRateFallback(Currency currency, ExchangeRateUnavailableException ex) {
        throw ex;
    }
}
