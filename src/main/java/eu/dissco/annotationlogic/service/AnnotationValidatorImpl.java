package eu.dissco.annotationlogic.service;

import static com.jayway.jsonpath.JsonPath.using;
import static eu.dissco.annotationlogic.utils.ValidationUtils.CLASS_MAP;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import eu.dissco.annotationlogic.domain.SelectorType;
import eu.dissco.annotationlogic.exception.InvalidAnnotationException;
import eu.dissco.annotationlogic.exception.InvalidAnnotationMotivationException;
import eu.dissco.annotationlogic.exception.InvalidTargetException;
import eu.dissco.annotationlogic.utils.ValidationUtils;
import eu.dissco.core.annotationlogic.schema.Annotation;
import eu.dissco.core.annotationlogic.schema.Annotation.OaMotivation;
import eu.dissco.core.annotationlogic.schema.DigitalMedia;
import eu.dissco.core.annotationlogic.schema.DigitalSpecimen;
import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotationValidatorImpl implements AnnotationValidator {

  private final ObjectMapper mapper;
  private final Configuration jsonPathConfig;
  private static final Pattern LAST_KEY_PATTERN = Pattern.compile("\\[(?!.*\\[')(.*)");
  private static final Pattern NUMERIC_PATTERN = Pattern.compile("\\d+");
  private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationValidatorImpl.class);

  public AnnotationValidatorImpl(ObjectMapper mapper, Configuration jsonPathConfig) {
    this.mapper = mapper;
    this.jsonPathConfig = jsonPathConfig;
  }


  @Override
  public boolean annotationIsValid(@Nonnull DigitalSpecimen digitalSpecimen,
      @Nonnull Annotation annotation)
      throws InvalidAnnotationException, InvalidTargetException {
    var context = using(jsonPathConfig).parse(getTargetAsString(digitalSpecimen));
    var pathIsValid = pathIsValid(context, annotation);
    var doesNotContainForbiddenFields = doesNotAnnotateForbiddenFields(annotation);
    var hasCorrectValueCount = annotationHasCorrectValueCount(annotation);
    applyTermAnnotation(context, annotation);
    return pathIsValid && doesNotContainForbiddenFields && hasCorrectValueCount;
  }

  @Override
  public boolean annotationIsValid(@Nonnull DigitalMedia digitalMedia,
      @Nonnull Annotation annotation)
      throws InvalidAnnotationException, InvalidTargetException {
    var context = using(jsonPathConfig).parse(getTargetAsString(digitalMedia));
    return pathIsValid(context, annotation);
  }

  @Override
  public DigitalSpecimen applyAnnotation(@Nonnull DigitalSpecimen digitalSpecimen,
      @Nonnull Annotation annotation)
      throws InvalidAnnotationException {
    return null;
  }

  @Override
  public DigitalMedia applyAnnotation(@Nonnull DigitalMedia digitalMedia,
      @Nonnull Annotation annotation)
      throws InvalidAnnotationException {
    return null;
  }

  @Override
  public DigitalSpecimen applyAnnotations(@Nonnull DigitalSpecimen digitalSpecimen,
      @Nonnull List<Annotation> annotations) throws InvalidAnnotationException {
    return null;
  }

  @Override
  public DigitalMedia applyAnnotations(@Nonnull DigitalMedia digitalMedia,
      @Nonnull List<Annotation> annotations)
      throws InvalidAnnotationException {
    return null;
  }

  private String getTargetAsString(DigitalSpecimen digitalSpecimen) throws InvalidTargetException {
    try {
      return mapper.writeValueAsString(digitalSpecimen);
    } catch (JsonProcessingException e) {
      throw new InvalidTargetException(e.getMessage());
    }
  }

  private String getTargetAsString(DigitalMedia digitalMedia) throws InvalidTargetException {
    try {
      return mapper.writeValueAsString(digitalMedia);
    } catch (JsonProcessingException e) {
      throw new InvalidTargetException(e.getMessage());
    }
  }

  private boolean doesNotAnnotateForbiddenFields(Annotation annotation) {
    var selector = getSelector(annotation);
    var lastKey = getLastKey(getTargetPath(annotation));
    if (SelectorType.CLASS_SELECTOR.equals(selector)) {
      return !ValidationUtils.FORBIDDEN_CLASSES.contains(lastKey);
    } else {
      return !ValidationUtils.FORBIDDEN_FIELDS.contains(lastKey);
    }
  }

  private boolean pathIsValid(DocumentContext context, Annotation annotation)
      throws InvalidAnnotationException {
    var path = getTargetPath(annotation);
    if (OaMotivation.OA_EDITING.equals(annotation.getOaMotivation())
        || OaMotivation.ODS_DELETING.equals(annotation.getOaMotivation())) {
      return (pathExists(context, path));
    } else if (OaMotivation.ODS_ADDING.equals(annotation.getOaMotivation())) {
      var parentPath = getParentPath(path);
      return (!pathExists(context, path) && pathExists(context, parentPath));
    } else {
      throw new InvalidAnnotationMotivationException(
          "Invalid motivation: " + annotation.getOaMotivation().toString());
    }
  }

  private static String getTargetPath(Annotation annotation) {
    var selector = getSelector(annotation);
    if (SelectorType.TERM_SELECTOR.equals(selector)) {
      return annotation.getOaHasTarget().getOaHasSelector().getAdditionalProperties()
          .get("ods:term").toString();
    } else if (SelectorType.CLASS_SELECTOR.equals(selector)) {
      return annotation.getOaHasTarget().getOaHasSelector().getAdditionalProperties()
          .get("ods:class").toString();
    } else {
      return "";
    }
  }

  private static Boolean annotationHasCorrectValueCount(Annotation annotation) {
    if (OaMotivation.ODS_DELETING.equals(annotation.getOaMotivation())) {
      return annotation.getOaHasBody().getOaValue().isEmpty();
    }
    return annotation.getOaHasBody().getOaValue().size() == 1;
  }

  private static Boolean pathExists(DocumentContext context, String path) {
    return context.read(path) != null;
  }

  private static SelectorType getSelector(Annotation annotation) {
    var selectorString = annotation.getOaHasTarget().getOaHasSelector().getAdditionalProperties()
        .get("@type").toString();
    return SelectorType.fromString(selectorString);
  }

  private static String getParentPath(String path) {
    return path.replaceAll(LAST_KEY_PATTERN.pattern(), "");
  }

  private static String getLastKey(String jsonPath) {
    var lastKeyMatcher = LAST_KEY_PATTERN.matcher(jsonPath);
    lastKeyMatcher.find();
    return lastKeyMatcher.group(1).replace("[", "").replace("]", "");
  }


  private String applyAnnotation(DocumentContext context, Annotation annotation) {
    var selectorType = getSelector(annotation);

    if (SelectorType.TERM_SELECTOR.equals(selectorType)) {
      return applyTermAnnotation(context, annotation);
    }
    return null; // todo
  }

  private String applyTermAnnotation(DocumentContext context, Annotation annotation) {
    var path = getTargetPath(annotation);
    if (annotation.getOaMotivation().equals(OaMotivation.ODS_DELETING)) {
      context.delete(path);
    } else {
      var parentPath = getParentPath(path);
      var lastKey = getLastKey(path);
      context.put(parentPath, lastKey, annotation.getOaHasBody().getOaValue().getFirst());
    }
    return context.toString();
  }

  private String applyClassAnnotation(DocumentContext context, Annotation annotation)
      throws InvalidAnnotationException {
    var path = getTargetPath(annotation);
    if (annotation.getOaMotivation().equals(OaMotivation.ODS_DELETING)) {
      context.delete(path);
    } else {
      var targetClass = getLastKey(path);
      if (NUMERIC_PATTERN.matcher(targetClass).matches()) {
        targetClass = getLastKey(getParentPath(targetClass)); // ignore the index for now
      }
      var clazz = CLASS_MAP.get(targetClass);
      if (clazz == null) {
        throw new InvalidAnnotationException("Unable to read selector path: " + path);
      }
      try {
        // Does the value we've been given map to the class we're interested in?
        var newObject = mapper.readValue(annotation.getOaHasBody().getOaValue().getFirst(), clazz);
      } catch (JsonProcessingException e) {
        LOGGER.error("Unable to read value {} as target class {}", annotation.getOaHasBody().getOaValue().getFirst(), targetClass, e);
        throw new  InvalidAnnotationException("Unable to read value " + annotation.getOaHasBody().getOaValue().getFirst() + " as class " + targetClass);
      }

      /*
      todo
      1. check if it's an array path (if adding, just append)
      2. Set desired value at the path
       */
    }

    return context.toString();
  }


  private boolean isArrayAnnotation(Annotation annotation) {
    var targetPath = getTargetPath(annotation);
    var lastKey = getLastKey(targetPath);
    return NUMERIC_PATTERN.matcher(lastKey).matches();
  }

  /*
  Considerations
  * if we're annotating an array of primitives, we don't append items to the array; the annotation
    should be an oa:editing annotation with the desired final state of the list.
  * We need to check if it's a root annotation before applying.


   */


}
