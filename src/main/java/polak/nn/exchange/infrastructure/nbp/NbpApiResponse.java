package polak.nn.exchange.infrastructure.nbp;

import java.math.BigDecimal;
import java.util.List;

public record NbpApiResponse(String table, String currency, String code, List<Rate> rates) {
    public record Rate(String no, String effectiveDate, BigDecimal mid) {
    }
}
