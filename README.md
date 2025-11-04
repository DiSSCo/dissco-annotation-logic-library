# DiSSCo Annotation Logic Library

This library is intended to validate annotations before being accepted by the DiSSCo infrastructure.
It is also used to apply valid annotations to a target object (either a digital specimen or digital
media).

## Usage

The annotation validator can be initialized using constructor injection:

```\java
@Service
@RequiredArgsConstructor
public class SomeClass {
    private final AnnotationValidator annotationValidator;
    
    public void checkAnnotation(DigitalSpecimen digitalSpecimen, Annotation annotation){
        var isValid = annotationValidator.annotationIsValid(digitalSpecimen, annotation);        
    }
    
    public DigitalSpecimen applyAnnotation(DigitalSpecimen digitalSpecimen, Annotation annotation) 
    throws InvalidAnnotationException, InvalidTargetException {
        return annotationValidator.applyAnnotation(digitalSpecimen, annotation);
    }
}
```

### Important

The `applyAnnotation()` method also validates annotations before applying them to the target. This
method will never return an object with an invalid annotation applied, **so it is not necessary to
use the annotationIsValid() method beforehand.**

Note:

- `applyAnnotation()` will throw an exception if the annotation or target is invalid.
- `annotationIsValid()` will return false instead of throwing any exceptions

## Annotation Validation Requirements

This section gives an overview of validation checks made on the annotation.

### 1. Annotation does not annotate forbidden fields

DiSSCo enforces a list of OpenDS fields that are considered system-managed and may not be altered
by external annotation services:

- ods:version
- dcterms:created
- dcterms:modified
- ods:midsLevel
- dcterms:identifier
- ods:fdoType
- ods:normalisedPhysicalSpecimenID
- ods:physicalSpecimenID
- ods:sourceSystemID
- ods:isKnownToContainMedia
- TombstoneMetadata (Whole class)

### 2. Target paths are valid

The annotation logic module needs to check verify the selector path based on the annotation's
motivation:

- For editing (`oa:editing`) or deleting (`ods:deleting`) an element, **the path must exist** in the
  current object.
- For adding (`ods:adding`) a new element, **the path must not exist**, but **the parent elements
  must be valid and present**. All fields in the path must be valid openDS terms.

### 3. The annotated target is valid openDS

The data being introduced or changed by an annotation is syntactically and semantically correct
according to the OpenDS schema. To ensure this, the annotation logic library preemptively
applies any incoming annotation to the target object. This modified target object is then
validated against the relevant JSON schema.

[DiSSCo JSON Schemas are publicly available](https://schemas.dissco.tech/schemas/fdo-type/).

### The annotation must target the provided target

The annotation validator checks the target of the annotation against the `dcterms:identifier` of the
provided target. These two values must match. 