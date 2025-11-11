package io.github.dissco.annotationlogic.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import io.github.dissco.annotationlogic.exception.InvalidAnnotationException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonSchemaValidator {

  private final JsonSchema specimenSchema;
  private final ObjectMapper mapper;
  private static final Logger LOGGER = LoggerFactory.getLogger(JsonSchemaValidator.class);

  public JsonSchemaValidator(JsonSchema specimenSchema, ObjectMapper mapper) {
    this.specimenSchema = specimenSchema;
    this.mapper = mapper;
  }

  public void specimenIsValid(String digitalSpecimenString) throws InvalidAnnotationException {
    JsonNode digitalSpecimen;
    try {
      digitalSpecimen = mapper.readTree(digitalSpecimenString);
    } catch (JsonProcessingException e) {
      LOGGER.warn("Unable to read resulting digital specimen", e);
      throw new InvalidAnnotationException("Unable to read resulting digital specimen");
    }
    var errors = specimenSchema.validate(digitalSpecimen);
    if (!errors.isEmpty()) {
      var errorMessage = setErrorMessage(errors);
      LOGGER.warn(errorMessage);
      throw new InvalidAnnotationException(errorMessage);
    }
  }

  private static String setErrorMessage(Set<ValidationMessage> validationErrors) {
    var errorBuilder = new StringBuilder()
        .append("Annotation produces invalid target. Errors: ");
    validationErrors.forEach(error -> errorBuilder.append(error.getMessage()).append(", "));
    return errorBuilder.toString();
  }

}
