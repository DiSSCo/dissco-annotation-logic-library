package io.github.dissco.annotationlogic.validator;

import io.github.dissco.annotationlogic.exception.InvalidAnnotationException;
import io.github.dissco.annotationlogic.exception.InvalidTargetException;
import io.github.dissco.core.annotationlogic.schema.Annotation;
import io.github.dissco.core.annotationlogic.schema.DigitalMedia;
import io.github.dissco.core.annotationlogic.schema.DigitalSpecimen;
import jakarta.annotation.Nonnull;

public interface AnnotationValidatorInterface {

  /**
   * Checks if an annotation is valid and if it would produce a valid target.
   *
   * @param target     Digital Specimen being annotated
   * @param annotation annotation to check
   * @return True if the annotation results in a valid target, false otherwise
   */
  public abstract boolean annotationIsValid(@Nonnull DigitalSpecimen target,
      @Nonnull Annotation annotation);

  /**
   * Checks if an annotation is valid and if it would produce a valid target.
   *
   * @param target     Digital Media being annotated
   * @param annotation annotation to check
   * @return True if the annotation results in a valid target, false otherwise
   */
  public abstract boolean annotationIsValid(@Nonnull DigitalMedia target,
      @Nonnull Annotation annotation);


  /**
   * Applies single annotation to a target digital specimen
   *
   * @param target     digital media being annotated
   * @param annotation annotation to apply
   * @return the target object with changes from the annotation, as a Digital Specimen
   * @throws InvalidAnnotationException If annotation is not valid
   */
  public abstract DigitalSpecimen applyAnnotation(@Nonnull DigitalSpecimen target,
      @Nonnull Annotation annotation)
      throws InvalidAnnotationException, InvalidTargetException;

  /**
   * Applies single annotation to a target digital specimen
   *
   * @param target     Digital Media being annotated
   * @param annotation annotation to apply
   * @return the target object with changes from the annotation, as a Digital Media
   * @throws InvalidAnnotationException If annotation is not valid
   */
  public abstract DigitalMedia applyAnnotation(@Nonnull DigitalMedia target,
      @Nonnull Annotation annotation)
      throws InvalidAnnotationException, InvalidTargetException;

}
