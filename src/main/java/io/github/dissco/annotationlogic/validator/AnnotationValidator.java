package io.github.dissco.annotationlogic.validator;

import static com.jayway.jsonpath.JsonPath.using;
import static io.github.dissco.annotationlogic.utils.ValidationUtils.CLASS_MAP;
import static io.github.dissco.annotationlogic.utils.ValidationUtils.SPECIMEN_PRIMITIVE_ARRAYS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotationValidator implements AnnotationValidatorInterface {

  private final ObjectMapper mapper;
  private final Configuration jsonPathConfig;
  private final JsonSchemaValidator jsonSchemaValidator;

  private static final Pattern KEY_PATTERN = Pattern.compile("\\['([^\"']+)[\"']");
  private static final Pattern LAST_KEY_OR_INDEX_PATTERN = Pattern.compile(
      "\\[(?:'([A-Za-z:]+)'|\\d+)]$");

  private static final Pattern ARRAY_PATTERN = Pattern.compile("^.+:\\w+s$");
  private static final Pattern ARRAY_OBJECTS_PATTERN = Pattern.compile("^.+:has\\w+s$");
  private static final Pattern OBJECT_PATTERN = Pattern.compile("^(.+:has\\w+(?<!s))$");

  private static final Pattern NUMERIC_PATTERN = Pattern.compile("\\d+");

  private static final Pattern BLOCK_SEGMENT_PATTERN = Pattern.compile("'([^']+)'|\\[(\\d+|\\*)]");
  private static final String WILDCARD_INDEX_PATTERN = "[*]";


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
      if (!pathExists(context, path) || path.contains(WILDCARD_INDEX_PATTERN)) {
        throw new InvalidAnnotationException(
            "Invalid path. Target path must exist for ods:editing and deleting annotations and may not contain wildcards.");
      }
    } else if (OaMotivation.ODS_ADDING.equals(annotation.getOaMotivation())) {
      if (!addingPathIsValid(context, path)) {
        throw new InvalidAnnotationException(
            "Invalid path. Target path must NOT exist for ods:adding annotation");
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

  // Verifies that paths containing explicit indexes exist. Overall path must not exist.
  private static boolean addingPathIsValid(DocumentContext context, String path) {
    if (pathExists(context, path) && !path.contains(WILDCARD_INDEX_PATTERN)) {
      return false;
    }
    var segments = getSegments(path);
    var currentPath = "$";
    for (var segment : segments) {
      currentPath = getNextPath(currentPath, segment);
      if (NUMERIC_PATTERN.matcher(segment).find() && !pathExists(context, currentPath)
          && !currentPath.contains(WILDCARD_INDEX_PATTERN)) {
        return false;
      }
    }
    return true;
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
        .replaceAll(LAST_KEY_OR_INDEX_PATTERN.pattern(), "");
  }

  private static String getLastKey(String jsonPath) {
    var keyMatcher = AnnotationValidator.KEY_PATTERN.matcher(jsonPath);
    String lastKey = "";
    while (keyMatcher.find()) {
      lastKey = keyMatcher.group(1);
    }
    return lastKey;
  }

  private String applyAnnotationToContext(DocumentContext context, Annotation annotation)
      throws InvalidAnnotationException {
    var selectorType = getSelector(annotation);
    return applyAnnotation(context, annotation, SelectorType.CLASS_SELECTOR.equals(selectorType));
  }

  private String applyAnnotation(DocumentContext context, Annotation annotation,
      boolean isClassAnnotation)
      throws InvalidAnnotationException {
    var path = getTargetPath(annotation);
    if (OaMotivation.ODS_DELETING.equals(annotation.getOaMotivation())) {
      context.delete(path);
    } else {
      var annotationValueObject = isClassAnnotation ?
          mapAnnotationValueToTargetClass(path, annotation) :
          mapAnnotationValueToTargetField(path, annotation);
      if (OaMotivation.OA_EDITING.equals(annotation.getOaMotivation())) {
        context.set(path, annotationValueObject);
      } else {
        var parentPath = getParentPath(path);
        parentPath = addParent(context, parentPath);
        applyAddAnnotation(context, path, parentPath, annotationValueObject);
      }
    }
    return context.jsonString();
  }

  private Map<?, ?> mapAnnotationValueToTargetClass(String path, Annotation annotation)
      throws InvalidAnnotationException {
    var targetClassString = getLastKey(path);
    var targetClass = CLASS_MAP.get(targetClassString);
    if (targetClass == null) {
      LOGGER.warn("Unrecognized class: {}", targetClassString);
      throw new InvalidAnnotationException("Unrecognized class: " + path);
    }
    try {
      // Checks if the value of the annotation correctly maps to its intended class
      var newObject = mapper.readValue(annotation.getOaHasBody().getOaValue().getFirst(),
          targetClass);
      return mapper.convertValue(newObject, Map.class);
    } catch (JsonProcessingException e) {
      LOGGER.error("Unable to read value {} as target class {}",
          annotation.getOaHasBody().getOaValue().getFirst(), targetClass, e);
      throw new InvalidAnnotationBodyException(
          "Unable to read value " + annotation.getOaHasBody().getOaValue().getFirst()
              + " as class " + targetClass);
    }
  }

  private Object mapAnnotationValueToTargetField(String path, Annotation annotation)
      throws InvalidAnnotationException {
    var value = annotation.getOaHasBody().getOaValue().getFirst();
    var key = getLastKey(path);
    if (SPECIMEN_PRIMITIVE_ARRAYS.contains(key)) {
      try {
        return mapper.readValue(value, new TypeReference<List<String>>() {
        });
      } catch (JsonProcessingException e) {
        LOGGER.warn("Unable to read {} as array", value, e);
        throw new InvalidAnnotationException(
            "Unable to read " + value + "as array for field " + key);
      }
    }
    return value;
  }

  private void applyAddAnnotation(DocumentContext context, String path, String parentPath,
      Object newValue) {
    // If we're appending a class to the end of an array
    if (path.endsWith(WILDCARD_INDEX_PATTERN)) {
      context.set(parentPath, newValue);
    } else {
      var newField = getLastKey(path);
      context.put(parentPath, newField, newValue);
    }
  }

  private String addParent(DocumentContext context, String parentPath) {
    var pathList = getSegments(parentPath);
    var currentPath = "$";
    for (int i = 0; i < pathList.size(); i++) {
      var segment = pathList.get(i);
      if ("*".equals(segment)) {
        // If it's a wild card, select the next index in the given array
        segment = context.read(currentPath + ".length()").toString();
      }
      var nextPath = getNextPath(currentPath, segment);
      if (context.read(nextPath) == null) {
        if (!NUMERIC_PATTERN.matcher(segment).matches()) {
          // Add the next segment
          addChildSegment(context, currentPath, segment);
        } else { // We're at an index, so we add a value to an array
          var previousSegment = pathList.get(i - 1);
          // If it's an array of objects, we create a hashmap
          if (ARRAY_OBJECTS_PATTERN.matcher(previousSegment).matches()) {
            context.add(currentPath, new HashMap<>());
          }
        }
      }
      currentPath = nextPath;
    }
    return currentPath;
  }

  private static String getNextPath(String currentPath, String segment) {
    return NUMERIC_PATTERN.matcher(segment).matches() ?
        currentPath + "[" + segment + "]"
        : currentPath + "['" + segment + "']";
  }

  private static List<String> getSegments(String path) {
    return BLOCK_SEGMENT_PATTERN.matcher(path)
        .results()
        .map(m -> m.group(1) != null ? m.group(1) : m.group(2))
        .toList();
  }

  private static void addChildSegment(DocumentContext context, String currentPath, String segment) {
    // Case 1: Child is an array
    if (ARRAY_PATTERN.matcher(segment).matches()) {
      context.put(currentPath, segment, new ArrayList<>()); // Put an empty list
    } else if (OBJECT_PATTERN.matcher(segment).matches()) {
      // Case 2: Child is an object
      context.put(currentPath, segment, new HashMap<>()); // Put an empty map
    }
  }


}
