package io.github.dissco.annotationlogic.validator;

import static io.github.dissco.annotationlogic.TestUtils.MAPPER;
import static io.github.dissco.annotationlogic.TestUtils.MEDIA_ID;
import static io.github.dissco.annotationlogic.TestUtils.NEW_VALUE;
import static io.github.dissco.annotationlogic.TestUtils.SPECIMEN_ID;
import static io.github.dissco.annotationlogic.TestUtils.givenAnnotation;
import static io.github.dissco.annotationlogic.TestUtils.givenAnnotationTarget;
import static io.github.dissco.annotationlogic.TestUtils.givenDigitalSpecimen;
import static io.github.dissco.annotationlogic.TestUtils.givenEvent;
import static io.github.dissco.annotationlogic.TestUtils.givenIdentification;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.jayway.jsonpath.Option;
import io.github.dissco.annotationlogic.exception.InvalidAnnotationException;
import io.github.dissco.core.annotationlogic.schema.Annotation;
import io.github.dissco.core.annotationlogic.schema.Annotation.OaMotivation;
import io.github.dissco.core.annotationlogic.schema.AnnotationBody;
import io.github.dissco.core.annotationlogic.schema.AnnotationTarget;
import io.github.dissco.core.annotationlogic.schema.DigitalSpecimen;
import io.github.dissco.core.annotationlogic.schema.GeologicalContext;
import io.github.dissco.core.annotationlogic.schema.Location;
import io.github.dissco.core.annotationlogic.schema.OaHasSelector;
import io.github.dissco.core.annotationlogic.schema.TaxonIdentification;
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
class SpecimenAnnotationValidatorTest {

  private AnnotationValidator annotationValidator;
  @Mock
  private JsonSchemaValidator jsonSchemaValidator;

  @BeforeEach
  void setUp() {
    annotationValidator = new AnnotationValidator(MAPPER,
        com.jayway.jsonpath.Configuration.builder()
            .options(Option.SUPPRESS_EXCEPTIONS)
            .build(), jsonSchemaValidator);
  }


  @ParameterizedTest
  @MethodSource("invalidAnnotations")
  void testInvalidAnnotation(Annotation annotation) {

    // Then
    assertThrows(InvalidAnnotationException.class,
        () -> annotationValidator.applyAnnotation(givenDigitalSpecimen(), annotation));
  }

  @Test
  void testInvalidResult() throws InvalidAnnotationException {

    // Given
    doThrow(InvalidAnnotationException.class).when(jsonSchemaValidator).specimenIsValid(any());

    // When
    assertThrows(InvalidAnnotationException.class,
        () -> annotationValidator.applyAnnotation(givenDigitalSpecimen(), givenAnnotation()));
  }

  @ParameterizedTest
  @MethodSource("validAnnotationsAndResult")
  void testApplyAnnotations(Annotation annotation, DigitalSpecimen expected) throws Exception {
    // Given

    // When
    var result = annotationValidator.applyAnnotation(givenDigitalSpecimen(), annotation);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  private static Stream<Arguments> validAnnotationsAndResult() {
    return Stream.of(
        Arguments.of(
            givenAnnotation(OaMotivation.OA_EDITING, true),
            givenDigitalSpecimen()
                .withOdsHasEvents(List.of(
                    givenEvent()
                        .withOdsHasLocation(new Location()
                            .withDwcCountry(NEW_VALUE))
                ))
        ),
        Arguments.of(
            givenAnnotation(OaMotivation.OA_EDITING, false),
            givenDigitalSpecimen()
                .withOdsHasIdentifications(List.of(
                    givenIdentification()
                        .withOdsHasTaxonIdentifications(List.of(
                            new TaxonIdentification()
                                .withDwcGenus(NEW_VALUE)
                                .withDwcPhylum(NEW_VALUE)
                        ))
                ))
        ),
        Arguments.of(
            givenAnnotation(OaMotivation.ODS_ADDING, true),
            givenDigitalSpecimen()
                .withOdsHasEvents(List.of(
                    givenEvent()
                        .withOdsHasLocation(givenEvent().getOdsHasLocation()
                            .withDwcLocality(NEW_VALUE))))
        ),
        Arguments.of(
            givenAnnotation(OaMotivation.ODS_ADDING, false),
            givenDigitalSpecimen()
                .withOdsHasIdentifications(List.of(givenIdentification()
                    .withOdsHasTaxonIdentifications(
                        List.of(
                            givenIdentification().getOdsHasTaxonIdentifications().getFirst(),
                            new TaxonIdentification()
                                .withDwcGenus(NEW_VALUE)
                                .withDwcPhylum(NEW_VALUE)
                        )
                    )
                ))
        ),
        Arguments.of(
            givenAnnotation(OaMotivation.OA_EDITING, false),
            givenDigitalSpecimen()
                .withOdsHasIdentifications(List.of(givenIdentification()
                    .withOdsHasTaxonIdentifications(List.of(
                        new TaxonIdentification()
                            .withDwcGenus(NEW_VALUE)
                            .withDwcPhylum(NEW_VALUE)
                    ))))
        ),
        Arguments.of(
            givenAnnotation(OaMotivation.OA_EDITING, false)
                .withOaHasTarget(localityTargetEdit())
                .withOaHasBody(localityBody()),
            givenDigitalSpecimen()
                .withOdsHasEvents(List.of(givenEvent()
                    .withOdsHasLocation(
                        new Location()
                            .withDwcCountry(NEW_VALUE)
                            .withDwcLocality(NEW_VALUE)
                    )))
        ),
        Arguments.of(
            givenAnnotation(OaMotivation.ODS_ADDING, false)
                .withOaHasTarget(geologicalContextAdd())
                .withOaHasBody(geologicalContext()),
            givenDigitalSpecimen()
                .withOdsHasEvents(List.of(givenEvent().withOdsHasLocation(
                    givenEvent().getOdsHasLocation()
                        .withOdsHasGeologicalContext(
                            new GeologicalContext().withDwcLithostratigraphicTerms(NEW_VALUE)))
                ))
        ),
        Arguments.of(
            givenAnnotation(OaMotivation.ODS_DELETING, false),
            givenDigitalSpecimen()
                .withOdsHasIdentifications(
                    List.of(givenIdentification().withOdsHasTaxonIdentifications(List.of())))
        ),
        Arguments.of(
            givenAnnotation(OaMotivation.ODS_DELETING, true),
            givenDigitalSpecimen()
                .withOdsHasEvents(List.of(givenEvent().withOdsHasLocation(new Location()))))
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
            givenAnnotation(OaMotivation.ODS_ADDING, true)
                .withOaHasTarget(
                    givenAnnotationTarget(
                        "$['ods:hasCitations'][0]['dcterms:description']"
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
        Arguments.of(givenAnnotation(OaMotivation.OA_EDITING, false).withOaHasTarget(
            new AnnotationTarget()
                .withDctermsIdentifier(SPECIMEN_ID)
                .withType("ods:DigitalSpecimen")
                .withOaHasSelector(new OaHasSelector()
                    .withAdditionalProperty("@type", "ods:ClassSelector")
                    .withAdditionalProperty("ods:class", "$['ods:topicDiscipline']")
                )
        )),
        Arguments.of(
            givenAnnotation(OaMotivation.ODS_ADDING, false)
                .withOaHasBody(new AnnotationBody().withOaValue(List.of("value1")))
        ),
        Arguments.of(
            givenAnnotation(OaMotivation.ODS_ADDING, false)
                .withOaHasBody(new AnnotationBody().withOaValue(List.of("""
                    {
                      "someField": "someValue"
                    }
                    """)))
        ),
        Arguments.of(givenAnnotation(OaMotivation.OA_COMMENTING, false))
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
          "dwc:country": "Some new value!",
          "dwc:locality" : "Some new value!"
        }
        """));
  }

  private static AnnotationBody geologicalContext() {
    return new AnnotationBody().withOaValue(List.of(
        """
            {
              "dwc:lithostratigraphicTerms" : "Some new value!"
            }
            """
    ));
  }


}
