package polak.nn.account.infrastructure.persistence.account;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import polak.nn.account.domain.model.Currency;

@Converter
public class BalancesJsonConverter implements AttributeConverter<Map<Currency, BigDecimal>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final TypeReference<EnumMap<Currency, BigDecimal>> TYPE_REF = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(Map<Currency, BigDecimal> attribute) {
        try {
            Map<Currency, BigDecimal> value = attribute == null ? new EnumMap<>(Currency.class) : attribute;
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize balances map to JSON", e);
        }
    }

    @Override
    public Map<Currency, BigDecimal> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isBlank()) {
                return new EnumMap<>(Currency.class);
            }
            return OBJECT_MAPPER.readValue(dbData, TYPE_REF);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot deserialize balances JSON", e);
        }
    }
}
