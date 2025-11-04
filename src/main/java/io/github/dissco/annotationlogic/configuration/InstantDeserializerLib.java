package io.github.dissco.annotationlogic.configuration;

import static io.github.dissco.annotationlogic.configuration.ApplicationConfigurationLib.FORMATTER;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstantDeserializerLib extends JsonDeserializer<Instant> {

  private static final Logger LOGGER = LoggerFactory.getLogger(InstantDeserializerLib.class);

  @Override
  public Instant deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
    try {
      return Instant.from(FORMATTER.parse(jsonParser.getText()));
    } catch (IOException e) {
      LOGGER.error("An error has occurred deserializing a date. More information: {}", e.getMessage());
      return null;
    }
  }
}
