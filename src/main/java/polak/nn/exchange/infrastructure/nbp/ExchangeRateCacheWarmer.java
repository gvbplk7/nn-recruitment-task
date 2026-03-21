package polak.nn.exchange.infrastructure.nbp;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExchangeRateCacheWarmer {

    private final NbpExchangeRateProvider exchangeRateProvider;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        exchangeRateProvider.warmUpCache();
    }
}
