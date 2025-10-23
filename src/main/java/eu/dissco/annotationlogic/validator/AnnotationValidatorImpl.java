package eu.dissco.annotationlogic.validator;

import static com.jayway.jsonpath.JsonPath.using;
import static eu.dissco.annotationlogic.utils.ValidationUtils.CLASS_MAP;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import eu.dissco.annotationlogic.domain.SelectorType;
import eu.dissco.annotationlogic.exception.InvalidAnnotationBodyException;
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
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotationValidatorImpl implements AnnotationValidator {

  private final ObjectMapper mapper;
  private final Configuration jsonPathConfig;
  private final JsonSchemaValidator jsonSchemaValidator;
  private static final String LAST_INDEX_PATTERN = "\\[(?!.*\\[)(\\d+)]";
  private static final Pattern LAST_KEY_PATTERN = Pattern.compile("\\[(?!.*\\[')(.*)']");
  private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationValidatorImpl.class);

  public AnnotationValidatorImpl(ObjectMapper mapper, Configuration jsonPathConfig, JsonSchemaValidator jsonSchemaValidator) {
    this.mapper = mapper;
    this.jsonPathConfig = jsonPathConfig;
    this.jsonSchemaValidator = jsonSchemaValidator;
  }


  @Override
  public boolean annotationIsValid(@Nonnull DigitalSpecimen digitalSpecimen,
      @Nonnull Annotation annotation) {
    try {
      var context = using(jsonPathConfig).parse(getTargetAsString(digitalSpecimen));
      var initialPass = preapplicationChecks(context, annotation);
      var annotatedTarget = applyAnnotationToContext(context, annotation);
      var annotatedTargetIsValid = jsonSchemaValidator.specimenIsValid(annotatedTarget);
      return initialPass && annotatedTargetIsValid;
    } catch (InvalidAnnotationException | InvalidTargetException e) {
      return false;
    }
  }

  @Override
  public boolean annotationIsValid(@Nonnull DigitalMedia digitalMedia,
      @Nonnull Annotation annotation) {
    return false;
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

  private boolean preapplicationChecks(DocumentContext context, Annotation annotation)
      throws InvalidAnnotationException {
    var identifier = (String) context.read("$['dcterms:identifier']");
    var annotationTargetsObject = annotationTargetsObject(annotation, identifier);
    var pathIsValid = pathIsValid(context, annotation);
    var doesNotContainForbiddenFields = doesNotAnnotateForbiddenFields(annotation);
    var hasCorrectValueCount = annotationHasCorrectValueCount(annotation);
    return annotationTargetsObject && pathIsValid && doesNotContainForbiddenFields
        && hasCorrectValueCount;

  }


  private String getTargetAsString(DigitalSpecimen digitalSpecimen) throws InvalidTargetException {
    try {
      return mapper.writeValueAsString(digitalSpecimen);
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

  private boolean annotationTargetsObject(Annotation annotation, String targetId) {
    return Objects.equals(targetId, annotation.getOaHasTarget().getDctermsIdentifier());
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
    return context.toString();
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
    return context.toString();
  }

  private void applyClassAnnotationAdd(DocumentContext context, String path,
      Map<String, Object> newClassValue) {
    var arrPath = path.replaceAll(LAST_INDEX_PATTERN, ""); // remove trailing index for adding
    var parentPath = getParentPath(arrPath);
    var arr = context.read(arrPath);
    var arrayContext = using(jsonPathConfig).parse(arr);
    arrayContext.add("$", newClassValue);
    context.set(parentPath, arrayContext);
  }

}
