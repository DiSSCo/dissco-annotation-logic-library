package eu.dissco.annotationlogic.configuration;


import static eu.dissco.annotationlogic.configuration.ApplicationConfiguration.FORMATTER;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DateDeserializer extends JsonDeserializer<Date> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DateDeserializer.class);

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
