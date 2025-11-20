package io.github.dissco.annotationlogic.utils;


import io.github.dissco.core.annotationlogic.schema.Agent;
import io.github.dissco.core.annotationlogic.schema.Assertion;
import io.github.dissco.core.annotationlogic.schema.ChronometricAge;
import io.github.dissco.core.annotationlogic.schema.Citation;
import io.github.dissco.core.annotationlogic.schema.EntityRelationship;
import io.github.dissco.core.annotationlogic.schema.GeologicalContext;
import io.github.dissco.core.annotationlogic.schema.Georeference;
import io.github.dissco.core.annotationlogic.schema.Identification;
import io.github.dissco.core.annotationlogic.schema.Identifier;
import io.github.dissco.core.annotationlogic.schema.Location;
import io.github.dissco.core.annotationlogic.schema.OdsHasRelatedPID;
import io.github.dissco.core.annotationlogic.schema.OdsHasRole;
import io.github.dissco.core.annotationlogic.schema.SpecimenPart;
import io.github.dissco.core.annotationlogic.schema.TaxonIdentification;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.events.Event;

public class ValidationUtils {

  private ValidationUtils() {
    // Utility class
  }

  public static final Set<String> FORBIDDEN_FIELDS = Set.of(
      "ods:version",
      "dcterms:created",
      "dcterms:modified",
      "ods:midsLevel",
      "dcterms:identifier",
      "ods:fdoType",
      "ods:normalisedPhysicalSpecimenID",
      "odsphysicalSpecimenID",
      "ods:isKnownToContainMedia");

  public static final Set<String> FORBIDDEN_CLASSES = Set.of(
      "ods:hasTombstoneMetadata"
  );

  public static final Map<String, Class<?>> CLASS_MAP;

  static {
    CLASS_MAP = Map.ofEntries
        (Map.entry("ods:hasAgents", Agent.class),
            Map.entry("ods:hasAssertions", Assertion.class),
            Map.entry("ods:hasChronometricAges", ChronometricAge.class),
            Map.entry("ods:hasCitations", Citation.class),
            Map.entry("ods:hasEntityRelationships", EntityRelationship.class),
            Map.entry("ods:hasEvents", Event.class),
            Map.entry("ods:hasGeologicalContext", GeologicalContext.class),
            Map.entry("ods:hasGeoreference", Georeference.class),
            Map.entry("ods:hasIdentifications", Identification.class),
            Map.entry("ods:hasIdentifiers", Identifier.class),
            Map.entry("ods:hasLocation", Location.class),
            Map.entry("ods:hasRelatedPIDs", OdsHasRelatedPID.class),
            Map.entry("ods:hasRoles", OdsHasRole.class),
            Map.entry("ods:hasSpecimenParts", SpecimenPart.class),
            Map.entry("ods:hasTaxonIdentifications", TaxonIdentification.class)
        );
  }


}
