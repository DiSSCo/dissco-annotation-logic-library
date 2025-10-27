package com.github.dissco.annotationlogic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class AnnotationLogicApplication {

  public static void main(String[] args) {
    SpringApplication.run(AnnotationLogicApplication.class, args);
  }

}
