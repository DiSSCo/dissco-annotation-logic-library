package eu.dissco.annotationlogic.utils;

import eu.dissco.core.annotationlogic.schema.Agent;
import eu.dissco.core.annotationlogic.schema.Assertion;
import eu.dissco.core.annotationlogic.schema.ChronometricAge;
import eu.dissco.core.annotationlogic.schema.Citation;
import eu.dissco.core.annotationlogic.schema.EntityRelationship;
import eu.dissco.core.annotationlogic.schema.GeologicalContext;
import eu.dissco.core.annotationlogic.schema.Identification;
import eu.dissco.core.annotationlogic.schema.Identifier;
import eu.dissco.core.annotationlogic.schema.Location;
import eu.dissco.core.annotationlogic.schema.OdsHasRelatedPID;
import eu.dissco.core.annotationlogic.schema.OdsHasRole;
import eu.dissco.core.annotationlogic.schema.SpecimenPart;
import eu.dissco.core.annotationlogic.schema.TaxonIdentification;
import java.util.HashMap;
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
      "ods:modsLevel",
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
    var map = new HashMap<String, Class<?>>();
    map.put("ods:hasAgents", Agent.class);
    map.put("ods:hasAssertions", Assertion.class);
    map.put("ods:hasChronometricAges", ChronometricAge.class);
    map.put("ods:hasCitations", Citation.class);
    map.put("ods:hasEntityRelationships", EntityRelationship.class);
    map.put("ods:hasEvents", Event.class);
    map.put("ods:hasGeologicalContext", GeologicalContext.class);
    map.put("ods:hasIdentifications", Identification.class);
    map.put("ods:hasIdentifiers", Identifier.class);
    map.put("ods:hasLocation", Location.class);
    map.put("ods:hasRelatedPIDs", OdsHasRelatedPID.class);
    map.put("ods:hasRoles", OdsHasRole.class);
    map.put("ods:hasSpecimenParts", SpecimenPart.class);
    map.put("ods:hasTaxonIdentifications", TaxonIdentification.class);
    CLASS_MAP = map;
  }


}
