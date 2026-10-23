package com.ecommerce.backend.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

/** Stock/version numbers must be JSON integers, never rounded decimals or strings. */
public class InventarioIntegerDeserializer extends JsonDeserializer<Integer> {
    @Override public Integer deserialize(JsonParser p, DeserializationContext context) throws IOException {
        if (p.currentToken() != JsonToken.VALUE_NUMBER_INT) {
            return (Integer) context.handleUnexpectedToken(Integer.class, p);
        }
        return p.getIntValue();
    }
}
