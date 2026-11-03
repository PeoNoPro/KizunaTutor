package com.kizuna.features.ticket.dto;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
public class TicketVersionDeserializer extends JsonDeserializer<Long> {
 @Override public Long deserialize(JsonParser p,DeserializationContext c)throws IOException{
   if(p.currentToken()!=JsonToken.VALUE_NUMBER_INT)return (Long)c.handleUnexpectedToken(Long.class,p);
   long v=p.getLongValue();if(v<0)throw new JsonMappingException(p,"Phiên bản không hợp lệ.");return v;
 }
}
