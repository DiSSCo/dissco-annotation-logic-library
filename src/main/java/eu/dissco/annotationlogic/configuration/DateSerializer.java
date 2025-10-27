package eu.dissco.annotationlogic.configuration;

import static eu.dissco.annotationlogic.configuration.ApplicationConfiguration.FORMATTER;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateSerializer extends JsonSerializer<Date> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DateSerializer.class);

  @Override
  public void serialize(Date value, JsonGenerator jsonGenerator,
      SerializerProvider serializerProvider) {
    try {
      jsonGenerator.writeString(FORMATTER.format(value.toInstant()));
    } catch (IOException e) {
      LOGGER.error("An error has occurred serializing a date. More information: {}", e.getMessage());
    }
  }
}
