package eu.dissco.annotationlogic.validator;

import static eu.dissco.annotationlogic.TestUtils.MAPPER;
import static eu.dissco.annotationlogic.TestUtils.givenDigitalSpecimen;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import eu.dissco.annotationlogic.TestUtils;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JsonSchemaValidatorTest {

  private static final JsonSchemaFactory FACTORY = JsonSchemaFactory.getInstance(VersionFlag.V202012);
  private JsonSchemaValidator jsonSchemaValidator;

  @BeforeEach
  void setup() throws IOException {
    var schemaUrl = "json-schema/digital-specimen.json";
    try (var input = Thread.currentThread().getContextClassLoader().getResourceAsStream(schemaUrl)) {
      var schema = FACTORY.getSchema(input);
      jsonSchemaValidator = new JsonSchemaValidator(schema, MAPPER);
    }
  }

  @Test
  void testValidSpecimen() throws Exception {
    // Given
    var specimen = MAPPER.writeValueAsString(givenDigitalSpecimen());

    // When
    var result = jsonSchemaValidator.specimenIsValid(specimen);

    // Then
    assertThat(result).isTrue();
  }

  @ParameterizedTest
  @MethodSource("invalidSpecimen")
  void testInvalidSpecimen(String digitalSpecimenString) {
    // Given

    // When
    var result = jsonSchemaValidator.specimenIsValid(digitalSpecimenString);

    // Then
    assertThat(result).isFalse();
  }

  private static Stream<Arguments> invalidSpecimen() throws IOException {
    var jsonNodeSpecimen = (ObjectNode) MAPPER.valueToTree(TestUtils.givenDigitalSpecimen());
    var missingRequiredValueSpecimen = (ObjectNode) MAPPER.valueToTree(TestUtils.givenDigitalSpecimen());
    missingRequiredValueSpecimen.remove("dcterms:identifier");

    return Stream.of(
        Arguments.of(
            MAPPER.writeValueAsString(jsonNodeSpecimen.put("unknownField", "unknownValue"))
        ),
        Arguments.of(
            MAPPER.writeValueAsString(jsonNodeSpecimen.put("ods:topicDiscipline", "unknownValue"))
        ),
        Arguments.of(
            MAPPER.writeValueAsString(missingRequiredValueSpecimen)
        ),
        Arguments.of("not a specimen")
    );
  }


}
