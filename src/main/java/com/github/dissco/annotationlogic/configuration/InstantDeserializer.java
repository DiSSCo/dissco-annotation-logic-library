package com.github.dissco.annotationlogic.configuration;

import static com.github.dissco.annotationlogic.configuration.ApplicationConfiguration.FORMATTER;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstantDeserializer extends JsonDeserializer<Instant> {

  private static final Logger LOGGER = LoggerFactory.getLogger(InstantDeserializer.class);

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
