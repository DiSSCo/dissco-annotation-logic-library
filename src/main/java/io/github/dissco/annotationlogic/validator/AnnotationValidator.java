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

public class AnnotationValidator implements AnnotationValidatorInterface {

  private final ObjectMapper mapper;
  private final Configuration jsonPathConfig;
  private final JsonSchemaValidator jsonSchemaValidator;
  private static final Pattern LAST_INDEX_PATTERN = Pattern.compile("\\[(?!.*\\[)(\\d+)]");
  private static final Pattern LAST_KEY_PATTERN = Pattern.compile("\\[(?!.*\\[[\"'])(.*)[\"']]");
  private static final Pattern BLOCK_NOTATION_PATTERN = Pattern.compile(
      "^\\$((?:\\[['\"][A-Za-z:]+['\"]])+(?:\\[\\d+])*+)*+");
  private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationValidator.class);

  public AnnotationValidator(ObjectMapper mapper, Configuration jsonPathConfig,
      JsonSchemaValidator jsonSchemaValidator) {
    this.mapper = mapper;
    this.jsonPathConfig = jsonPathConfig;
    this.jsonSchemaValidator = jsonSchemaValidator;
  }

  public DigitalSpecimen applyAnnotation(@Nonnull DigitalSpecimen digitalSpecimen,
      @Nonnull Annotation annotation)
      throws InvalidAnnotationException, InvalidTargetException {
    var target = getTargetAsString(digitalSpecimen);
    var context = using(jsonPathConfig).parse(target);
    preapplicationChecks(context, annotation);
    var annotatedTarget = applyAnnotationToContext(context, annotation);
    try {
      jsonSchemaValidator.specimenIsValid(annotatedTarget);
      return mapper.readValue(annotatedTarget, DigitalSpecimen.class);
    } catch (JsonProcessingException e) {
      LOGGER.warn("Unable to parse annotated target", e);
      throw new InvalidAnnotationException("Unable to parse annotated target");
    }
  }

  @Override
  public DigitalMedia applyAnnotation(@Nonnull DigitalMedia target, @Nonnull Annotation annotation)
      throws InvalidAnnotationException, InvalidTargetException {
    throw new UnsupportedOperationException("Media validation not yet supported");
  }

  private static void preapplicationChecks(DocumentContext context, Annotation annotation)
      throws InvalidAnnotationException {
    var identifier = (String) context.read("$['dcterms:identifier']");
    annotationTargetsObject(annotation, identifier);
    pathIsValid(context, annotation);
    doesNotAnnotateForbiddenFields(annotation);
    annotationHasCorrectValueCount(annotation);
  }


  private <T> String getTargetAsString(T target) throws InvalidTargetException {
    try {
      return mapper.writeValueAsString(target);
    } catch (JsonProcessingException e) {
      throw new InvalidTargetException(e.getMessage());
    }
  }

  private static void doesNotAnnotateForbiddenFields(Annotation annotation)
      throws InvalidAnnotationException {
    var selector = getSelector(annotation);
    var lastKey = getLastKey(getTargetPath(annotation));
    if (SelectorType.CLASS_SELECTOR.equals(selector) && ValidationUtils.FORBIDDEN_CLASSES.contains(
        lastKey)) {
      throw new InvalidAnnotationException(
          "Annotation is attempting to annotate class" + lastKey + ", which is forbidden");
    } else if (SelectorType.TERM_SELECTOR.equals(selector)
        && ValidationUtils.FORBIDDEN_FIELDS.contains(lastKey)) {
      throw new InvalidAnnotationException(
          "Annotation is attempting to annotate term " + lastKey + ", which is forbidden");
    }
  }

  private static void annotationTargetsObject(Annotation annotation, String targetId)
      throws InvalidAnnotationException {
    if (!Objects.equals(targetId, annotation.getOaHasTarget().getDctermsIdentifier())) {
      throw new InvalidAnnotationException("Annotation does not target provided target");
    }
  }

  private static void pathIsValid(DocumentContext context, Annotation annotation)
      throws InvalidAnnotationException {
    var path = getTargetPath(annotation);
    if (!BLOCK_NOTATION_PATTERN.matcher(path).find()) {
      throw new InvalidAnnotationException("Selector path is not in valid JSON path format");
    }
    if ((OaMotivation.OA_EDITING.equals(annotation.getOaMotivation())
        || OaMotivation.ODS_DELETING.equals(annotation.getOaMotivation()))) {
      if (!pathExists(context, path)) {
        throw new InvalidAnnotationException(
            "Invalid path. Target path must exist for ods:editing annotation");
      }
    } else if (OaMotivation.ODS_ADDING.equals(annotation.getOaMotivation())) {
      var parentPath = getParentPath(path);
      if (pathExists(context, path) || !pathExists(context, parentPath)) {
        throw new InvalidAnnotationException(
            "Invalid path. Target path must NOT exist for ods:adding annotation, but parent path must exist. Use a class selector instead.");
      }
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

  private static void annotationHasCorrectValueCount(Annotation annotation)
      throws InvalidAnnotationException {
    if (OaMotivation.ODS_DELETING.equals(annotation.getOaMotivation()) && !annotation.getOaHasBody()
        .getOaValue().isEmpty()) {
      throw new InvalidAnnotationException("Deleting annotations must not have any value");
    } else if ((OaMotivation.ODS_ADDING.equals(annotation.getOaMotivation())
        || OaMotivation.OA_EDITING.equals(annotation.getOaMotivation())) &&
        annotation.getOaHasBody().getOaValue().size() != 1) {
      throw new InvalidAnnotationException(
          "Editing or adding annotations must have exactly one value");
    }
  }

  private static boolean pathExists(DocumentContext context, String path) {
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
        .replace("'", "")
        .replace("\"", "");
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
