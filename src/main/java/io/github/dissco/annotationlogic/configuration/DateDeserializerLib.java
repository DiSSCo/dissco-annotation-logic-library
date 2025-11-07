package io.github.dissco.annotationlogic.configuration;


import static io.github.dissco.annotationlogic.utils.DateUtils.FORMATTER;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DateDeserializerLib extends JsonDeserializer<Date> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DateDeserializerLib.class);

  @Override
  public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
    try {
      return Date.from(Instant.from(FORMATTER.parse(jsonParser.getText())));
    } catch (IOException e) {
      LOGGER.error("An error has occurred deserializing a date. More information: {}", e.getMessage());
      return null;
    }
  }
}
