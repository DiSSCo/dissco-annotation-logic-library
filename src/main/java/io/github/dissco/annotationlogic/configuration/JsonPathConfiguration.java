package io.github.dissco.annotationlogic.configuration;

import com.jayway.jsonpath.Option;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonPathConfiguration {

  @Bean
  public com.jayway.jsonpath.Configuration jsonPathConfiguration() {
    return com.jayway.jsonpath.Configuration.builder()
        .options(Option.SUPPRESS_EXCEPTIONS)
        .build();
  }

}
