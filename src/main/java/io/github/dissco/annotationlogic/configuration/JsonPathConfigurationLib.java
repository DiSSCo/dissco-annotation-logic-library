package io.github.dissco.annotationlogic.configuration;

import com.jayway.jsonpath.Option;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonPathConfigurationLib {

  @Bean("jsonPathConfigLib")
  public com.jayway.jsonpath.Configuration jsonPathConfigLib() {
    return com.jayway.jsonpath.Configuration.builder()
        .options(Option.SUPPRESS_EXCEPTIONS)
        .build();
  }

}
