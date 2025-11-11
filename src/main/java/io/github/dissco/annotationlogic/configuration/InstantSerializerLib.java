package io.github.dissco.annotationlogic.configuration;


import static io.github.dissco.annotationlogic.utils.DateUtils.FORMATTER;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstantSerializerLib extends JsonSerializer<Instant> {

  private static final Logger LOGGER = LoggerFactory.getLogger(InstantSerializerLib.class);

  @Override
  public void serialize(Instant value, JsonGenerator jsonGenerator,
      SerializerProvider serializerProvider) {
    try {
      jsonGenerator.writeString(FORMATTER.format(value));
    } catch (IOException e) {
      LOGGER.error("An error has occurred serializing a date. More information: {}", e.getMessage());
    }
  }
}
