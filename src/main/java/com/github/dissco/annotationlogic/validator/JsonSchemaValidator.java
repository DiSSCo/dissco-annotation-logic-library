package com.github.dissco.annotationlogic.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import java.util.Arrays;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

public class JsonSchemaValidator {

  private final JsonSchema specimenSchema;
  private final ObjectMapper mapper;
  private static final Logger LOGGER = LoggerFactory.getLogger(JsonSchemaValidator.class);

  public JsonSchemaValidator(@Qualifier("specimenSchema") JsonSchema specimenSchema, ObjectMapper mapper) {
    this.specimenSchema = specimenSchema;
    this.mapper = mapper;
  }

  public boolean specimenIsValid(String digitalSpecimenString) {
    JsonNode digitalSpecimen;
    try {
      digitalSpecimen = mapper.readTree(digitalSpecimenString);
    } catch (JsonProcessingException e){
      LOGGER.warn("Unable to read resulting digital specimen", e);
      return false;
    }
    var errors = specimenSchema.validate(digitalSpecimen);
    if (!errors.isEmpty()) {
      var errorMessage = setErrorMessage(errors, "digital specimen", digitalSpecimen);
      LOGGER.warn(errorMessage);
      return false;
    }
    return true;
  }

  private static String setErrorMessage(Set<ValidationMessage> validationErrors, String objectType,
      JsonNode targetObject) {
    var errorBuilder = new StringBuilder()
        .append("Annotation produces invalid ")
        .append(objectType);
    for (var validationError : validationErrors) {
      if (validationError.getType().equals("required")) {
        errorBuilder.append("\nMissing attributes: ")
            .append(Arrays.toString(validationError.getArguments()));
      } else if (validationError.getType().equals("additionalProperties")) {
        errorBuilder.append("\nUnrecognized attributes: ")
            .append(Arrays.toString(validationError.getArguments()));
      } else if (validationError.getType().equals("enum")) {
        errorBuilder.append("\nEnum errors: ")
            .append(validationError.getMessage())
            .append(". Invalid value:")
            .append(getProblemEnumValue(targetObject, validationError.getPath()));
      } else {
        errorBuilder.append("\nOther errors: ")
            .append(validationError.getMessage());
      }
    }
    return errorBuilder.toString();
  }

  private static String getProblemEnumValue(JsonNode targetObject, String path) {
    path = path.replace("$.", "");
    try {
      return targetObject.get(path).asText();
    } catch (NullPointerException npe) {
      return "Unable to parse problem enum value";
    }
  }


}
