package eu.dissco.annotationlogic.service;

import eu.dissco.annotationlogic.exception.InvalidAnnotationException;
import eu.dissco.annotationlogic.exception.InvalidTargetException;
import eu.dissco.core.annotationlogic.schema.Annotation;
import eu.dissco.core.annotationlogic.schema.DigitalMedia;
import eu.dissco.core.annotationlogic.schema.DigitalSpecimen;
import jakarta.annotation.Nonnull;
import java.util.List;

public interface AnnotationValidator {


  /**
   * Checks if an annotation is valid and if it would produce a valid digital specimen.
   *
   * @param digitalSpecimen digital specimen object being annotated
   * @param annotation      annotation to check
   * @return True if the object's type is valid for the annotation, false otherwise.
   * @throws InvalidAnnotationException if annotation can not be applied to a target (e.g. incorrect
   *                                    motivation)
   * @throws InvalidTargetException     if target can not be parsed
   */
  boolean annotationIsValid(@Nonnull DigitalSpecimen digitalSpecimen,
      @Nonnull Annotation annotation) throws InvalidAnnotationException, InvalidTargetException;

  /**
   * Checks if an annotation is valid and if it would produce a valid media.
   *
   * @param digitalMedia digital media object being annotated
   * @param annotation   annotation to check
   * @return True if the object's type is valid for the annotation, false otherwise.
   * @throws InvalidAnnotationException if annotation can not be applied to a target (e.g.
   *                                    motivation is not adding, editing, or deleting; or selector
   *                                    is ROI selector)
   * @throws InvalidTargetException     if target can not be parsed
   */
  boolean annotationIsValid(@Nonnull DigitalMedia digitalMedia, @Nonnull Annotation annotation)
      throws InvalidAnnotationException, InvalidTargetException;

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
      throws InvalidAnnotationException;

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

  /**
   * Applies a list of annotations to a target object Applies annotations from oldest to newest
   *
   * @param digitalSpecimen digital specimen being annotated
   * @param annotations     annotations to apply.
   * @return Target object with changes from the annotation
   * @throws InvalidAnnotationException If any annotation is not valid
   */
  DigitalSpecimen applyAnnotations(@Nonnull DigitalSpecimen digitalSpecimen,
      @Nonnull List<Annotation> annotations) throws InvalidAnnotationException;

  /**
   * Applies a list of annotations to a target object Applies annotations from oldest to newest
   *
   * @param digitalMedia digital media being annotated
   * @param annotations  annotations to apply.
   * @return Target object with changes from the annotation
   * @throws InvalidAnnotationException If any annotation is not valid
   */
  DigitalMedia applyAnnotations(@Nonnull DigitalMedia digitalMedia,
      @Nonnull List<Annotation> annotations)
      throws InvalidAnnotationException;


}
