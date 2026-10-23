package com.ecommerce.backend.technicaldraft;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

/** A version cannot be silently truncated from a float or coerced from a string. */
public class DraftVersionDeserializer extends JsonDeserializer<Long> {
    @Override public Long deserialize(JsonParser parser,DeserializationContext context) throws IOException {
        if(parser.currentToken()!=JsonToken.VALUE_NUMBER_INT)throw new DraftFault(400,"La versión debe ser un entero no negativo.");
        long value=parser.getLongValue();if(value<0)throw new DraftFault(400,"La versión debe ser un entero no negativo.");return value;
    }
}
