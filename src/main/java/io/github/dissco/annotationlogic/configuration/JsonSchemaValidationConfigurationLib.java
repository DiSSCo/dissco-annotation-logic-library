package io.github.dissco.annotationlogic.configuration;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonSchemaValidationConfigurationLib {

  private static final JsonSchemaFactory FACTORY = JsonSchemaFactory.getInstance(VersionFlag.V202012);

  @Bean("mediaSchemaLib")
  public JsonSchema mediaSchema() throws IOException {
    var schema = "json-schema/digital-media.json";
    try (var input = Thread.currentThread().getContextClassLoader().getResourceAsStream(schema)) {
      return FACTORY.getSchema(input);
    }
  }

  @Bean("specimenSchemaLib")
  public JsonSchema specimenSchema() throws IOException {
    var schema = "json-schema/digital-specimen.json";
    try (var input = Thread.currentThread().getContextClassLoader().getResourceAsStream(schema)) {
      return FACTORY.getSchema(input);
    }
  }

}
