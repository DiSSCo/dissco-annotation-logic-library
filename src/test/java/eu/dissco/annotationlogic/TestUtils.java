package eu.dissco.annotationlogic;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import eu.dissco.annotationlogic.config.DateDeserializer;
import eu.dissco.annotationlogic.config.DateSerializer;
import eu.dissco.annotationlogic.config.InstantDeserializer;
import eu.dissco.annotationlogic.config.InstantSerializer;
import eu.dissco.core.annotationlogic.schema.Agent;
import eu.dissco.core.annotationlogic.schema.Agent.Type;
import eu.dissco.core.annotationlogic.schema.Annotation;
import eu.dissco.core.annotationlogic.schema.Annotation.OaMotivation;
import eu.dissco.core.annotationlogic.schema.Annotation.OdsStatus;
import eu.dissco.core.annotationlogic.schema.AnnotationBody;
import eu.dissco.core.annotationlogic.schema.AnnotationTarget;
import eu.dissco.core.annotationlogic.schema.DigitalSpecimen;
import eu.dissco.core.annotationlogic.schema.DigitalSpecimen.OdsLivingOrPreserved;
import eu.dissco.core.annotationlogic.schema.DigitalSpecimen.OdsPhysicalSpecimenIDType;
import eu.dissco.core.annotationlogic.schema.DigitalSpecimen.OdsTopicDiscipline;
import eu.dissco.core.annotationlogic.schema.EntityRelationship;
import eu.dissco.core.annotationlogic.schema.Event;
import eu.dissco.core.annotationlogic.schema.Location;
import eu.dissco.core.annotationlogic.schema.OaHasSelector;
import eu.dissco.core.annotationlogic.schema.OdsHasRole;
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
  static {
    var mapper = new ObjectMapper().findAndRegisterModules();
    SimpleModule dateModule = new SimpleModule();
    dateModule.addSerializer(Instant.class, new InstantSerializer());
    dateModule.addDeserializer(Instant.class, new InstantDeserializer());
    dateModule.addSerializer(Date.class, new DateSerializer());
    dateModule.addDeserializer(Date.class, new DateDeserializer());
    mapper.registerModule(dateModule);
    mapper.setSerializationInclusion(Include.NON_NULL);
    MAPPER = mapper.copy();
  }


  public static DigitalSpecimen givenDigitalSpecimen(){
    return new DigitalSpecimen()
        .withOdsOrganisationID("https://ror.org/039zvsn29")
        .withOdsOrganisationName("National Museum of Natural History")
        .withOdsPhysicalSpecimenIDType(OdsPhysicalSpecimenIDType.RESOLVABLE)
        .withOdsPhysicalSpecimenID("https://data.biodiversitydata.nl/naturalis/specimen/ZMA.INS.1003070")
        .withOdsNormalisedPhysicalSpecimenID("https://data.biodiversitydata.nl/naturalis/specimen/ZMA.INS.1003070")
        .withOdsTopicDiscipline(OdsTopicDiscipline.BOTANY)
        .withOdsSourceSystemID(HANDLE_ID)
        .withOdsSourceSystemName("A Source System")
        .withOdsLivingOrPreserved(OdsLivingOrPreserved.PRESERVED)
        .withDctermsModified("2022-11-01T09:59:24.000Z")
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
            new Event()
                .withDwcEventDate("2022-11-01T09:59:24.000Z")
                .withOdsHasLocation(new Location()
                    .withDwcCountry("England"))
        ));
  }

  public static Annotation givenAnnotationTerm(OaMotivation motivation){
    return new Annotation()
        .withId(HANDLE_ID)
        .withDctermsIdentifier(HANDLE_ID)
        .withType("ods:Annotation")
        .withOdsFdoType(FDO_TYPE)
        .withOdsVersion(1)
        .withOdsStatus(OdsStatus.ACTIVE)
        .withOaHasBody(givenOaBodyTerm())
        .withOaMotivation(motivation)
        .withOaHasTarget(givenOaTargetTerm(motivation))
        .withDctermsCreator(givenAgent(Type.PROV_PERSON))
        .withDctermsCreated(Date.from(CREATED))
        .withDctermsIssued(Date.from(CREATED))
        .withDctermsModified(Date.from(CREATED))
        .withAsGenerator(givenAgent(Type.PROV_SOFTWARE_AGENT));
  }

  private static AnnotationBody givenOaBodyTerm(){
    return new AnnotationBody()
        .withType("oa:TextualBody")
        .withOaValue(new ArrayList<>(List.of("Some new value!")))
        .withDctermsReferences(
            "https://medialib.naturalis.nl/file/id/ZMA.UROCH.P.1555/format/large")
        .withOdsScore(0.99);
  }

  private static AnnotationTarget givenOaTargetTerm(OaMotivation motivation) {
    var path = OaMotivation.OA_EDITING.equals(motivation) ?
        "$['ods:hasEvents'][0]['ods:hasLocation']['dwc:country']" :
        "$['ods:hasEvents'][0]['ods:hasLocation']['dwc:locality']";
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

  private static Agent givenAgent(Type type) {
    return new Agent()
        .withSchemaName("Some agent")
        .withId(HANDLE_ID)
        .withType(type);
  }



}
