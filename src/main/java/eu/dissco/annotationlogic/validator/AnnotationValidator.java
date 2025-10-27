package eu.dissco.annotationlogic.validator;

import eu.dissco.annotationlogic.exception.InvalidAnnotationException;
import eu.dissco.annotationlogic.exception.InvalidTargetException;
import eu.dissco.core.annotationlogic.schema.Annotation;
import eu.dissco.core.annotationlogic.schema.DigitalMedia;
import eu.dissco.core.annotationlogic.schema.DigitalSpecimen;
import jakarta.annotation.Nonnull;

public interface AnnotationValidator {


  /**
   * Checks if an annotation is valid and if it would produce a valid digital specimen.
   *
   * @param digitalSpecimen digital specimen object being annotated
   * @param annotation      annotation to check
   * @return True if the annotation results in a valid target, false otherwise
   */
  boolean annotationIsValid(@Nonnull DigitalSpecimen digitalSpecimen,
      @Nonnull Annotation annotation);

  /**
   * Checks if an annotation is valid and if it would produce a valid media.
   *
   * @param digitalMedia digital media object being annotated
   * @param annotation   annotation to check
   * @return True if the annotation results in a valid target, false otherwise.
   */
  boolean annotationIsValid(@Nonnull DigitalMedia digitalMedia, @Nonnull Annotation annotation);

  /**
   * Applies single annotation to a target digital specimen
   *
   * @param digitalSpecimen digital specimen being annotated
   * @param annotation      annotation to apply
   * @return the target object with changes from the annotation
   * @throws InvalidAnnotationException If annotation is not valid
   */
  DigitalSpecimen applyAnnotation(@Nonnull DigitalSpecimen digitalSpecimen,
      @Nonnull Annotation annotation)
      throws InvalidAnnotationException, InvalidTargetException;

  /**
   * Applies single annotation to a target digital media
   *
   * @param digitalMedia digital specimen being annotated
   * @param annotation   annotation to apply
   * @return the target object with changes from the annotation
   * @throws InvalidAnnotationException If annotation is not valid
   */
  DigitalMedia applyAnnotation(@Nonnull DigitalMedia digitalMedia, @Nonnull Annotation annotation)
      throws InvalidAnnotationException;


}
