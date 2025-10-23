package eu.dissco.annotationlogic.validator;

import static eu.dissco.annotationlogic.TestUtils.MAPPER;
import static eu.dissco.annotationlogic.TestUtils.MEDIA_ID;
import static eu.dissco.annotationlogic.TestUtils.SPECIMEN_ID;
import static eu.dissco.annotationlogic.TestUtils.givenAnnotation;
import static eu.dissco.annotationlogic.TestUtils.givenAnnotationTarget;
import static eu.dissco.annotationlogic.TestUtils.givenDigitalSpecimen;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.jayway.jsonpath.Option;
import eu.dissco.core.annotationlogic.schema.Annotation;
import eu.dissco.core.annotationlogic.schema.Annotation.OaMotivation;
import eu.dissco.core.annotationlogic.schema.AnnotationBody;
import eu.dissco.core.annotationlogic.schema.AnnotationTarget;
import eu.dissco.core.annotationlogic.schema.OaHasSelector;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnotationValidatorImplTest {

  private AnnotationValidatorImpl annotationValidator;
  @Mock
  private JsonSchemaValidator jsonSchemaValidator;

  @BeforeEach
  void setUp() {
    annotationValidator = new AnnotationValidatorImpl(MAPPER,
        com.jayway.jsonpath.Configuration.builder()
            .options(Option.SUPPRESS_EXCEPTIONS)
            .build(), jsonSchemaValidator);
  }

  @ParameterizedTest
  @MethodSource("validAnnotations")
  void testValidAnnotation(Annotation annotation) {
    // Given
    given(jsonSchemaValidator.specimenIsValid(any())).willReturn(true);

    // When
    var result = annotationValidator.annotationIsValid(givenDigitalSpecimen(), annotation);

    // Then
    assertThat(result).isTrue();
  }

  @ParameterizedTest
  @MethodSource("invalidAnnotations")
  void testInvalidAnnotation(Annotation annotation) {
    // When
    var result = annotationValidator.annotationIsValid(givenDigitalSpecimen(), annotation);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void testInvalidResult() {
    // Given
    given(jsonSchemaValidator.specimenIsValid(any())).willReturn(false);

    // When
    var result = annotationValidator.annotationIsValid(givenDigitalSpecimen(), givenAnnotation());

    // Then
    assertThat(result).isFalse();
  }


  private static Stream<Arguments> validAnnotations() {
    return Stream.of(
        Arguments.of(
            givenAnnotation(OaMotivation.OA_EDITING, true)
        ),
        Arguments.of(
            givenAnnotation(OaMotivation.OA_EDITING, false)
        ),
        Arguments.of(
            givenAnnotation(OaMotivation.ODS_ADDING, true)
        ),
        Arguments.of(
            givenAnnotation(OaMotivation.ODS_ADDING, false)
        ),
        Arguments.of(
            givenAnnotation(OaMotivation.OA_EDITING, false)
        ),
        Arguments.of(
            givenAnnotation(OaMotivation.OA_EDITING, false)
                .withOaHasTarget(localityTargetEdit())
                .withOaHasBody(localityBody())
        ),
        Arguments.of(
            givenAnnotation(OaMotivation.ODS_ADDING, false)
                .withOaHasTarget(geologicalContextAdd())
                .withOaHasBody(geologicalContext())
        )
    );
  }


  private static Stream<Arguments> invalidAnnotations() {
    return Stream.of(
        Arguments.of(
            givenAnnotation()
                .withOaHasTarget(
                    givenAnnotationTarget("$['ods:hasEvents'][0]['ods:hasLocation']['dwc:country']")
                        .withDctermsIdentifier(MEDIA_ID)
                )
        ),
        Arguments.of(
            givenAnnotation()
                .withOaHasTarget(
                    givenAnnotationTarget(
                        "$['dwc:pathDoesNotExist']"
                    )
                )
        ),
        Arguments.of(
            givenAnnotation(OaMotivation.ODS_DELETING, true)
                .withOaHasTarget(
                    givenAnnotationTarget(
                        "$['dwc:pathDoesNotExist']"
                    )
                )
        ),
        Arguments.of(
            givenAnnotation(OaMotivation.ODS_ADDING, true)
                .withOaHasTarget(
                    givenAnnotationTarget(
                        "$['ods:topicDiscipline']"
                    )
                )
        ),
        Arguments.of(
            givenAnnotation(OaMotivation.ODS_DELETING, true)
                .withOaHasTarget(
                    givenAnnotationTarget(
                        "$['dcterms:identifier']"
                    )
                )
        ),
        Arguments.of(givenAnnotation().withOaHasBody(new AnnotationBody().withOaValue(List.of()))),
        Arguments.of(givenAnnotation().withOaHasBody(
            new AnnotationBody().withOaValue(List.of("value1", "value2")))),
        Arguments.of(givenAnnotation()),
        Arguments.of(givenAnnotation(OaMotivation.OA_EDITING, false).withOaHasTarget(
            new AnnotationTarget()
                .withDctermsIdentifier(SPECIMEN_ID)
                .withType("ods:DigitalSpecimen")
                .withOaHasSelector(new OaHasSelector()
                    .withAdditionalProperty("@type", "ods:ClassSelector")
                    .withAdditionalProperty("ods:class", "$['ods:topicDiscipline']")
                )
        ))
    );

  }


  private static AnnotationTarget localityTargetEdit() {
    return new AnnotationTarget()
        .withDctermsIdentifier(SPECIMEN_ID)
        .withType("ods:DigitalSpecimen")
        .withOaHasSelector(
            new OaHasSelector()
                .withAdditionalProperty("@type", "ods:ClassSelector")
                .withAdditionalProperty("ods:class", "$['ods:hasEvents'][0]['ods:hasLocation']"));
  }

  private static AnnotationTarget geologicalContextAdd() {
    return new AnnotationTarget()
        .withDctermsIdentifier(SPECIMEN_ID)
        .withType("ods:DigitalSpecimen")
        .withOaHasSelector(
            new OaHasSelector()
                .withAdditionalProperty("@type", "ods:ClassSelector")
                .withAdditionalProperty("ods:class",
                    "$['ods:hasEvents'][0]['ods:hasLocation']['ods:hasGeologicalContext']"));
  }

  private static AnnotationBody localityBody() {
    return new AnnotationBody().withOaValue(List.of("""
        {
          "dwc:country": "anotherCountry",
          "dwc:locality" : "A new locality"
        }
        """));
  }

  private static AnnotationBody geologicalContext() {
    return new AnnotationBody().withOaValue(List.of(
        """
            {
              "dwc:lithostratigraphicTerms" : "Pleistocene-Weichselien"
            }
            """
    ));
  }


}
