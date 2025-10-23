package eu.dissco.annotationlogic.validator;

import static eu.dissco.annotationlogic.TestUtils.MAPPER;

import com.jayway.jsonpath.Option;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
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




}
