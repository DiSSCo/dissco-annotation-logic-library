package eu.dissco.annotationlogic.config;


import static eu.dissco.annotationlogic.config.ApplicationConfig.FORMATTER;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstantSerializer extends JsonSerializer<Instant> {

  Logger logger = LoggerFactory.getLogger(InstantSerializer.class);

  @Override
  public void serialize(Instant value, JsonGenerator jsonGenerator,
      SerializerProvider serializerProvider) {
    try {
      jsonGenerator.writeString(FORMATTER.format(value));
    } catch (IOException e) {
      logger.error("An error has occurred serializing a date. More information: {}", e.getMessage());
    }
  }
}
