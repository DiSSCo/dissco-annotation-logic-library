package io.github.dissco.annotationlogic.configuration;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.jayway.jsonpath.Option;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import io.github.dissco.annotationlogic.validator.AnnotationValidator;
import io.github.dissco.annotationlogic.validator.JsonSchemaValidator;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnnotationLogicLibraryConfiguration {

  private static final JsonSchemaFactory FACTORY = JsonSchemaFactory.getInstance(
      VersionFlag.V202012);

  /**
   * Public bean exposed to consuming applications.
   *
   * @return the fully configured AnnotationValidator
   * @throws IOException if internal setup fails
   */
  @Bean
  public AnnotationValidator annotationValidator() throws IOException {
    return new AnnotationValidator(
        objectMapper(), jsonPathConfiguration(), jsonSchemaValidator()
    );
  }

  // --- Internal helper methods, private and not exposed as beans ---


  /**
   * Internal ObjectMapper used by the library.
   */
  private JsonSchemaValidator jsonSchemaValidator() throws IOException {
    return new JsonSchemaValidator(specimenSchema(), objectMapper());
  }

  /**
   * Internal JsonSchemaValidator, depends on internal ObjectMapper.
   */
  private com.jayway.jsonpath.Configuration jsonPathConfiguration() {
    return com.jayway.jsonpath.Configuration.builder()
        .options(Option.SUPPRESS_EXCEPTIONS)
        .build();
  }

  /**
   * Internal JsonPath configuration for library usage.
   */
  private ObjectMapper objectMapper() {
    var mapper = new ObjectMapper().findAndRegisterModules();
    SimpleModule dateModule = new SimpleModule();
    dateModule.addSerializer(Instant.class, new InstantSerializerLib());
    dateModule.addDeserializer(Instant.class, new InstantDeserializerLib());
    dateModule.addSerializer(Date.class, new DateSerializerLib());
    dateModule.addDeserializer(Date.class, new DateDeserializerLib());
    mapper.registerModule(dateModule);
    mapper.setSerializationInclusion(Include.NON_NULL);
    return mapper;
  }

  /**
   * Retrieve specimen JSON schema from local resource.
   */
  private JsonSchema specimenSchema() throws IOException {
    var schema = "json-schema/digital-specimen.json";
    try (var input = Thread.currentThread().getContextClassLoader().getResourceAsStream(schema)) {
      return FACTORY.getSchema(input);
    }
  }

}
