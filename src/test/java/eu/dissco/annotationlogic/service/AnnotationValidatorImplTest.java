package eu.dissco.annotationlogic.service;

import static com.jayway.jsonpath.JsonPath.using;
import static eu.dissco.annotationlogic.TestUtils.MAPPER;
import static eu.dissco.annotationlogic.TestUtils.givenAnnotationTerm;
import static eu.dissco.annotationlogic.TestUtils.givenDigitalSpecimen;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import eu.dissco.annotationlogic.exception.InvalidAnnotationException;
import eu.dissco.core.annotationlogic.schema.Annotation.OaMotivation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnnotationValidatorImplTest {

  private AnnotationValidatorImpl annotationValidator;

  @BeforeEach
  void setUp() {
    annotationValidator = new AnnotationValidatorImpl(MAPPER, com.jayway.jsonpath.Configuration.builder()
        .options(Option.SUPPRESS_EXCEPTIONS)
        .build());
  }

  @Test
  void testInvalidMotivationPath() throws Exception {
    // Given
    Configuration config = com.jayway.jsonpath.Configuration.builder()
        .options(Option.SUPPRESS_EXCEPTIONS).build();

    var addPath = "$['ods:hasEvents'][0]['ods:hasLocation']['dwc:country']";
    var editPath = "$['ods:hasEvents'][0]['ods:hasLocation']['dwc:locality']";

    // When
    var context = annotationValidator.annotationIsValid(givenDigitalSpecimen(), givenAnnotationTerm(OaMotivation.OA_EDITING));

    // Then


  }



}
