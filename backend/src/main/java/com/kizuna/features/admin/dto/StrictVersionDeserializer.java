package com.kizuna.features.admin.dto;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
public class StrictVersionDeserializer extends JsonDeserializer<Long> {
    @Override public Long deserialize(JsonParser p,DeserializationContext context) throws IOException {
        if(p.currentToken()!=JsonToken.VALUE_NUMBER_INT) return (Long)context.handleUnexpectedToken(Long.class,p);
        long value=p.getLongValue();
        if(value<0) throw new com.fasterxml.jackson.databind.JsonMappingException(p,"Phiên bản không hợp lệ.");
        return value;
    }
}
