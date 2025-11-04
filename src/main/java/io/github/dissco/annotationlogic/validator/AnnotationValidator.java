package io.github.dissco.annotationlogic.validator;

import static com.jayway.jsonpath.JsonPath.using;
import static io.github.dissco.annotationlogic.utils.ValidationUtils.CLASS_MAP;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import io.github.dissco.annotationlogic.domain.SelectorType;
import io.github.dissco.annotationlogic.exception.InvalidAnnotationBodyException;
import io.github.dissco.annotationlogic.exception.InvalidAnnotationException;
import io.github.dissco.annotationlogic.exception.InvalidAnnotationMotivationException;
import io.github.dissco.annotationlogic.exception.InvalidTargetException;
import io.github.dissco.annotationlogic.utils.ValidationUtils;
import io.github.dissco.core.annotationlogic.schema.Annotation;
import io.github.dissco.core.annotationlogic.schema.Annotation.OaMotivation;
import io.github.dissco.core.annotationlogic.schema.DigitalMedia;
import io.github.dissco.core.annotationlogic.schema.DigitalSpecimen;
import jakarta.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AnnotationValidator implements AnnotationValidatorInterface {

  private final ObjectMapper mapper;
  private final Configuration jsonPathConfig;
  private final JsonSchemaValidator jsonSchemaValidator;
  private static final Pattern LAST_INDEX_PATTERN = Pattern.compile("\\[(?!.*\\[)(\\d+)]");
  private static final Pattern LAST_KEY_PATTERN = Pattern.compile("\\[(?!.*\\[')(.*)']");
  private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationValidator.class);

  protected AnnotationValidator(ObjectMapper mapper, Configuration jsonPathConfig,
      JsonSchemaValidator jsonSchemaValidator) {
    this.mapper = mapper;
    this.jsonPathConfig = jsonPathConfig;
    this.jsonSchemaValidator = jsonSchemaValidator;
  }

  @Override
  public boolean annotationIsValid(@Nonnull DigitalSpecimen digitalSpecimen,
      @Nonnull Annotation annotation) {
    String target;
    try {
      target = mapper.writeValueAsString(digitalSpecimen);
      var context = using(jsonPathConfig).parse(target);
      var initialPass = preapplicationChecks(context, annotation);
      if (initialPass) {
        var annotatedTarget = applyAnnotationToContext(context, annotation);
        return jsonSchemaValidator.specimenIsValid(annotatedTarget);
      }
      return false;
    } catch (InvalidAnnotationException | JsonProcessingException e) {
      LOGGER.warn("An error has occurred processing the annotation", e);
      return false;
    }
  }

  @Override
  public boolean annotationIsValid(@Nonnull DigitalMedia target, @Nonnull Annotation annotation) {
    throw new UnsupportedOperationException("Media validation not yet supported");
  }

  public DigitalSpecimen applyAnnotation(@Nonnull DigitalSpecimen digitalSpecimen,
      @Nonnull Annotation annotation)
      throws InvalidAnnotationException, InvalidTargetException {
    var target = getTargetAsString(digitalSpecimen);
    var context = using(jsonPathConfig).parse(target);
    var initialPass = preapplicationChecks(context, annotation);
    if (initialPass) {
      var annotatedTarget = applyAnnotationToContext(context, annotation);
      if (jsonSchemaValidator.specimenIsValid(annotatedTarget)) {
        try {
          return mapper.readValue(annotatedTarget, DigitalSpecimen.class);
        } catch (JsonProcessingException e) {
          LOGGER.warn("Unable to parse annotated target", e);
          throw new InvalidAnnotationException("Unable to parse annotated target");
        }
      }
    }
    throw new InvalidAnnotationException("Annotation is not valid");
  }

  @Override
  public DigitalMedia applyAnnotation(@Nonnull DigitalMedia target, @Nonnull Annotation annotation)
      throws InvalidAnnotationException, InvalidTargetException {
    throw new UnsupportedOperationException("Media validation not yet supported");
  }

  private static boolean preapplicationChecks(DocumentContext context, Annotation annotation)
      throws InvalidAnnotationException {
    var identifier = (String) context.read("$['dcterms:identifier']");
    return
        annotationTargetsObject(annotation, identifier)
            && pathIsValid(context, annotation)
            && doesNotAnnotateForbiddenFields(annotation)
            && annotationHasCorrectValueCount(annotation);
  }


  private <T> String getTargetAsString(T target) throws InvalidTargetException {
    try {
      return mapper.writeValueAsString(target);
    } catch (JsonProcessingException e) {
      throw new InvalidTargetException(e.getMessage());
    }
  }

  private static boolean doesNotAnnotateForbiddenFields(Annotation annotation) {
    var selector = getSelector(annotation);
    var lastKey = getLastKey(getTargetPath(annotation));
    if (SelectorType.CLASS_SELECTOR.equals(selector)) {
      return !ValidationUtils.FORBIDDEN_CLASSES.contains(lastKey);
    } else {
      return !ValidationUtils.FORBIDDEN_FIELDS.contains(lastKey);
    }
  }

  private static boolean annotationTargetsObject(Annotation annotation, String targetId) {
    return Objects.equals(targetId, annotation.getOaHasTarget().getDctermsIdentifier());
  }

  private static boolean pathIsValid(DocumentContext context, Annotation annotation)
      throws InvalidAnnotationException {
    var path = getTargetPath(annotation);
    if (OaMotivation.OA_EDITING.equals(annotation.getOaMotivation())
        || OaMotivation.ODS_DELETING.equals(annotation.getOaMotivation())) {
      return pathExists(context, path);
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
    return path
        .replaceAll(LAST_INDEX_PATTERN.pattern(), "")
        .replaceAll(LAST_KEY_PATTERN.pattern(), "");
  }

  private static String getLastKey(String jsonPath) {
    var lastKeyMatcher = LAST_KEY_PATTERN.matcher(jsonPath);
    lastKeyMatcher.find();
    return lastKeyMatcher.group(1)
        .replace("[", "")
        .replace("]", "")
        .replace("'", "");
  }


  private String applyAnnotationToContext(DocumentContext context, Annotation annotation)
      throws InvalidAnnotationException {
    var selectorType = getSelector(annotation);
    if (SelectorType.TERM_SELECTOR.equals(selectorType)) {
      return applyTermAnnotation(context, annotation);
    } else {
      return applyClassAnnotation(context, annotation);
    }
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
    return context.jsonString();
  }

  private String applyClassAnnotation(DocumentContext context, Annotation annotation)
      throws InvalidAnnotationException {
    var path = getTargetPath(annotation);
    if (annotation.getOaMotivation().equals(OaMotivation.ODS_DELETING)) {
      context.delete(path);
    } else {
      var targetClass = getLastKey(path);
      var clazz = CLASS_MAP.get(targetClass);
      if (clazz == null) {
        LOGGER.warn("Unrecognized class: {}", targetClass);
        throw new InvalidAnnotationException("Unrecognized class: " + path);
      }
      Map<String, Object> newObjectHashMap;
      try {
        // Checks if the value of the annotation correctly maps to its intended class
        var newObject = mapper.readValue(annotation.getOaHasBody().getOaValue().getFirst(), clazz);
        newObjectHashMap = mapper.convertValue(newObject, Map.class);
      } catch (JsonProcessingException e) {
        LOGGER.error("Unable to read value {} as target class {}",
            annotation.getOaHasBody().getOaValue().getFirst(), targetClass, e);
        throw new InvalidAnnotationBodyException(
            "Unable to read value " + annotation.getOaHasBody().getOaValue().getFirst()
                + " as class " + targetClass);
      }
      if (OaMotivation.ODS_ADDING.equals(annotation.getOaMotivation())) {
        applyClassAnnotationAdd(context, path, newObjectHashMap);
      } else if (OaMotivation.OA_EDITING.equals(annotation.getOaMotivation())) {
        context.set(path, newObjectHashMap);
      }
    }
    return context.jsonString();
  }

  private void applyClassAnnotationAdd(DocumentContext context, String path,
      Map<String, Object> newClassValue) {
    // If we're appending a class to the end of an array
    if (LAST_INDEX_PATTERN.matcher(path).find()) {
      var arrPath = path.replaceAll(LAST_INDEX_PATTERN.pattern(), ""); // remove trailing index
      var arr = context.read(arrPath);
      var arrayContext = using(jsonPathConfig).parse(arr);
      arrayContext.add("$", newClassValue);
      context.set(arrPath, arrayContext.json());
    } else {
      var newField = getLastKey(path);
      var parentPath = getParentPath(path);
      context.put(parentPath, newField, newClassValue);
    }
  }

}
