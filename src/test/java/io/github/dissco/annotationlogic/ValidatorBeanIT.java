package io.github.dissco.annotationlogic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dissco.annotationlogic.configuration.AnnotationLogicLibraryConfiguration;
import io.github.dissco.annotationlogic.validator.AnnotationValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(classes = {AnnotationLogicLibraryConfiguration.class})
class ValidatorBeanIT {

  @Autowired
  private ApplicationContext context;

  @Test
  void testBeanExists(){
    // Given
    var validator = context.getBean(AnnotationValidator.class);

    // Then
    assertThat(validator).isNotNull();
    assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(ObjectMapper.class));
  }

}
