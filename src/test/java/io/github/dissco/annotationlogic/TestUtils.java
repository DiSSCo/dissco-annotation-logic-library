package io.github.dissco.annotationlogic;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.dissco.annotationlogic.configuration.DateDeserializerLib;
import io.github.dissco.annotationlogic.configuration.DateSerializerLib;
import io.github.dissco.annotationlogic.configuration.InstantDeserializerLib;
import io.github.dissco.annotationlogic.configuration.InstantSerializerLib;
import io.github.dissco.core.annotationlogic.schema.Agent;
import io.github.dissco.core.annotationlogic.schema.Agent.Type;
import io.github.dissco.core.annotationlogic.schema.Annotation;
import io.github.dissco.core.annotationlogic.schema.Annotation.OaMotivation;
import io.github.dissco.core.annotationlogic.schema.Annotation.OdsStatus;
import io.github.dissco.core.annotationlogic.schema.AnnotationBody;
import io.github.dissco.core.annotationlogic.schema.AnnotationTarget;
import io.github.dissco.core.annotationlogic.schema.DigitalSpecimen;
import io.github.dissco.core.annotationlogic.schema.DigitalSpecimen.OdsLivingOrPreserved;
import io.github.dissco.core.annotationlogic.schema.DigitalSpecimen.OdsPhysicalSpecimenIDType;
import io.github.dissco.core.annotationlogic.schema.DigitalSpecimen.OdsTopicDiscipline;
import io.github.dissco.core.annotationlogic.schema.EntityRelationship;
import io.github.dissco.core.annotationlogic.schema.Event;
import io.github.dissco.core.annotationlogic.schema.Identification;
import io.github.dissco.core.annotationlogic.schema.Location;
import io.github.dissco.core.annotationlogic.schema.OaHasSelector;
import io.github.dissco.core.annotationlogic.schema.OdsHasRole;
import io.github.dissco.core.annotationlogic.schema.TaxonIdentification;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TestUtils {

  public static final Instant CREATED = Instant.parse("2022-11-01T09:59:24.000Z");
  public static final String DOI_PROXY = "https://doi.org/";
  public static final String HANDLE_PROXY = "https://hdl.handle.net/";
  public static final String MEDIA_ID = DOI_PROXY + "10.3535/QRS-TUV-WYX";
  public static final String HANDLE_ID = HANDLE_PROXY + "20.5000.1025/111-222-333";
  public static final String SPECIMEN_ID = DOI_PROXY + "10.3535/AAA-BBB-CCC";
  public static final String FDO_TYPE = "https://doi.org/21.T11148/cf458ca9ee1d44a5608f";
  public static final ObjectMapper MAPPER;
  public static final String NEW_VALUE = "Some new value!";

  static {
    var mapper = new ObjectMapper().findAndRegisterModules();
    SimpleModule dateModule = new SimpleModule();
    dateModule.addSerializer(Instant.class, new InstantSerializerLib());
    dateModule.addDeserializer(Instant.class, new InstantDeserializerLib());
    dateModule.addSerializer(Date.class, new DateSerializerLib());
    dateModule.addDeserializer(Date.class, new DateDeserializerLib());
    mapper.registerModule(dateModule);
    mapper.setSerializationInclusion(Include.NON_NULL);
    MAPPER = mapper.copy();
  }


  public static DigitalSpecimen givenDigitalSpecimen() {
    return new DigitalSpecimen()
        .withId(SPECIMEN_ID)
        .withDctermsIdentifier(SPECIMEN_ID)
        .withOdsVersion(1)
        .withOdsFdoType(FDO_TYPE)
        .withDctermsCreated(Date.from(CREATED))
        .withOdsMidsLevel(1)
        .withType("ods:DigitalSpecimen")
        .withOdsOrganisationID("https://ror.org/039zvsn29")
        .withOdsOrganisationName("National Museum of Natural History")
        .withOdsPhysicalSpecimenIDType(OdsPhysicalSpecimenIDType.RESOLVABLE)
        .withOdsPhysicalSpecimenID(
            "https://data.biodiversitydata.nl/naturalis/specimen/ZMA.INS.1003070")
        .withOdsNormalisedPhysicalSpecimenID(
            "https://data.biodiversitydata.nl/naturalis/specimen/ZMA.INS.1003070")
        .withOdsTopicDiscipline(OdsTopicDiscipline.BOTANY)
        .withOdsSourceSystemID(HANDLE_ID)
        .withOdsSourceSystemName("A Source System")
        .withOdsLivingOrPreserved(OdsLivingOrPreserved.PRESERVED)
        .withDctermsModified("2022-11-01T09:59:24.000Z")
        .withOdsHasIdentifications(
            List.of(givenIdentification()
            )
        )
        .withOdsHasEntityRelationships(
            List.of(
                new EntityRelationship()
                    .withType("ods:EntityRelationship")
                    .withDwcRelationshipEstablishedDate(Date.from(CREATED))
                    .withDwcRelationshipOfResource("hasMedia")
                    .withOdsHasAgents(List.of(
                        new Agent()
                            .withType(Type.PROV_SOFTWARE_AGENT)
                            .withId(MEDIA_ID)
                            .withSchemaName("Processing service")
                            .withSchemaIdentifier(HANDLE_ID)
                            .withOdsHasRoles(List.of(new OdsHasRole().withType("schema:Role")
                                .withSchemaRoleName("ods:sourceSyste,")))
                    ))
                    .withDwcRelatedResourceID(MEDIA_ID)
                    .withOdsRelatedResourceURI(URI.create(MEDIA_ID))
            )
        )
        .withOdsHasEvents(List.of(
          givenEvent()
        ));
  }

  public static Event givenEvent() {
    return
        new Event()
            .withDwcEventDate("2022-11-01T09:59:24.000Z")
            .withOdsHasLocation(new Location()
                .withDwcCountry("England"));
  }

  public static Identification givenIdentification() {
    return new Identification()
        .withType("holotype")
        .withOdsHasTaxonIdentifications(List.of(
            new TaxonIdentification()
                .withDwcScientificName("Bombus bombus")
        ));

  }

  public static Annotation givenAnnotation() {
    return givenAnnotation(OaMotivation.OA_EDITING, true);
  }

  public static Annotation givenAnnotation(OaMotivation motivation, boolean isTermAnnotation) {
    var target = isTermAnnotation ? givenOaTargetTerm(motivation) : givenOaTargetClass(motivation);
    AnnotationBody body;
    if (motivation.equals(OaMotivation.ODS_DELETING)) {
      body = new AnnotationBody().withOaValue(List.of());
    } else {
      body = isTermAnnotation ? givenOaBodyTerm() : givenOaBodyClass();
    }

    return new Annotation()
        .withId(HANDLE_ID)
        .withDctermsIdentifier(HANDLE_ID)
        .withType("ods:Annotation")
        .withOdsFdoType(FDO_TYPE)
        .withOdsVersion(1)
        .withOdsStatus(OdsStatus.ACTIVE)
        .withOaHasBody(body)
        .withOaMotivation(motivation)
        .withOaHasTarget(target)
        .withDctermsCreator(givenAgent(Type.PROV_PERSON))
        .withDctermsCreated(Date.from(CREATED))
        .withDctermsIssued(Date.from(CREATED))
        .withDctermsModified(Date.from(CREATED))
        .withAsGenerator(givenAgent(Type.PROV_SOFTWARE_AGENT));
  }

  private static AnnotationBody givenOaBodyTerm() {
    return new AnnotationBody()
        .withType("oa:TextualBody")
        .withOaValue(new ArrayList<>(List.of(NEW_VALUE)))
        .withDctermsReferences(
            "https://medialib.naturalis.nl/file/id/ZMA.UROCH.P.1555/format/large")
        .withOdsScore(0.99);
  }

  private static AnnotationBody givenOaBodyClass() {
    return new AnnotationBody()
        .withType("oa:TextualBody")
        .withOaValue(new ArrayList<>(List.of("""
            {
              "dwc:genus": "Some new value!",
              "dwc:phylum": "Some new value!"
            }
            """)))
        .withDctermsReferences(
            "https://medialib.naturalis.nl/file/id/ZMA.UROCH.P.1555/format/large")
        .withOdsScore(0.99);
  }

  private static AnnotationTarget givenOaTargetTerm(OaMotivation motivation) {
    var path = OaMotivation.ODS_ADDING.equals(motivation) ?
        "$['ods:hasEvents'][0]['ods:hasLocation']['dwc:locality']" :
        "$['ods:hasEvents'][0]['ods:hasLocation']['dwc:country']";
    return givenAnnotationTarget(path);
  }

  public static AnnotationTarget givenAnnotationTarget(String path) {
    return new AnnotationTarget()
        .withId(SPECIMEN_ID)
        .withType("ods:DigitalSpecimen")
        .withOdsFdoType(FDO_TYPE)
        .withDctermsIdentifier(SPECIMEN_ID)
        .withOaHasSelector(
            new OaHasSelector()
                .withAdditionalProperty("ods:term", path)
                .withAdditionalProperty("@type", "ods:TermSelector")
        );
  }

  private static AnnotationTarget givenOaTargetClass(OaMotivation motivation) {
    var path = OaMotivation.ODS_ADDING.equals(motivation) ?
        "$['ods:hasIdentifications'][0]['ods:hasTaxonIdentifications'][1]" :
        "$['ods:hasIdentifications'][0]['ods:hasTaxonIdentifications'][0]";
    return new AnnotationTarget()
        .withId(SPECIMEN_ID)
        .withType("ods:DigitalSpecimen")
        .withOdsFdoType(FDO_TYPE)
        .withDctermsIdentifier(SPECIMEN_ID)
        .withOaHasSelector(
            new OaHasSelector()
                .withAdditionalProperty("ods:class", path)
                .withAdditionalProperty("@type", "ods:ClassSelector")
        );
  }

  private static Agent givenAgent(Type type) {
    return new Agent()
        .withSchemaName("Some agent")
        .withId(HANDLE_ID)
        .withType(type);
  }


}
