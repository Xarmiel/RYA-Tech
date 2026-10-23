package com.ecommerce.backend.technicaldraft;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TechnicalDraftValidatorTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private final TechnicalDraftValidator validator=new TechnicalDraftValidator(mapper);
    static ObjectNode document(ObjectMapper mapper) throws Exception {
        return (ObjectNode)mapper.readTree("""
          {"schema":"rya.technical-draft.v1","verificationStatus":"pending","requestedUse":"evaluate",
           "product":{"name":"CPU test","sku":"TEST-CPU","brand":"Test","type":"cpu","price":0,"stock":0},
           "technical":{"socket":"AM5","cores":8,"threads":16,"base_w":65,"igpu":false},
           "relatedRecords":{},"evidence":{"url":"https://example.com/spec","date":"2020-01-01","notes":"Revisar fuente"},"media":[]}
          """);
    }
    @Test void acceptsUnknownsWithoutInventingZeroAndPreservesFalse() throws Exception {
        var doc=document(mapper);((ObjectNode)doc.get("technical")).put("boost_mhz","   ");
        var result=validator.validate(doc,true);assertTrue(result.valid(),result.issues().toString());
        assertFalse(result.document().path("technical").path("igpu").asBoolean());assertTrue(result.document().path("technical").path("boost_mhz").isNull());
        assertEquals(0,result.document().path("product").path("price").intValue());
    }
    @Test void missingRequiredTechnicalFieldsCanBeDraftedButNotSubmitted() throws Exception {
        var doc=document(mapper);((ObjectNode)doc.get("technical")).remove("socket");
        assertTrue(validator.validate(doc,false).valid());assertFalse(validator.validate(doc,true).valid());
    }
    @Test void neverAcceptsClientClaimedVerificationOrResolvedRelations() throws Exception {
        var doc=document(mapper);doc.put("verificationStatus","verified");
        ((ObjectNode)doc.get("relatedRecords")).set("cpu_board",mapper.readTree("[{\"cpu_sku\":\"CPU\",\"board_sku\":\"BOARD\",\"resolutionStatus\":\"verified\"}]"));
        var result=validator.validate(doc,false);assertTrue(result.valid());assertEquals("pending",result.document().path("verificationStatus").asText());
        assertEquals("pending",result.document().path("relatedRecords").path("cpu_board").get(0).path("resolutionStatus").asText());
    }
    @Test void rejectsUnsupportedTypeMixedFieldsAndFractionalCounts() throws Exception {
        var doc=document(mapper);((ObjectNode)doc.get("technical")).put("cores",8.5).put("chipset","B650");
        assertFalse(validator.validate(doc,false).valid());((ObjectNode)doc.get("product")).put("type","not-a-type");assertFalse(validator.validate(doc,false).valid());
    }
    @Test void rejectsSocketOnlyCompatibilityAndInvalidRanges() throws Exception {
        var doc=document(mapper);((ObjectNode)doc.get("technical")).put("base_mhz",4000).put("boost_mhz",3000).put("threads",4);
        assertTrue(validator.validate(doc,true).issues().stream().filter(i->i.contains("Conflicto")).count()>=2);
    }
    @Test void requiresEvidenceAndRealNonFutureDates() throws Exception {
        var doc=document(mapper);var evidence=(ObjectNode)doc.get("evidence");
        for(String date:List.of("2025-02-30","2999-01-01")){evidence.put("date",date);assertFalse(validator.validate(doc,false).valid());}
        evidence.putNull("date");assertTrue(validator.validate(doc,false).valid());assertFalse(validator.validate(doc,true).valid());
    }
    @Test void rejectsInsecureCredentialedAndMalformedEvidenceUrls() throws Exception {
        var doc=document(mapper);for(String url:List.of("http://example.com","https://user:pass@example.com","javascript:alert(1)","https://")){
            ((ObjectNode)doc.get("evidence")).put("url",url);assertFalse(validator.validate(doc,false).valid());
        }
    }
    @Test void peripheralsCannotEnterPcCompatibilityEvaluation() throws Exception {
        var doc=document(mapper);((ObjectNode)doc.get("product")).put("type","peripheral");doc.set("technical",mapper.createObjectNode());
        assertFalse(validator.validate(doc,true).valid());doc.put("requestedUse","catalog");assertTrue(validator.validate(doc,true).valid());
    }
    @Test void rejectsUnknownAuthorityFieldsAndNonObjectStructures() throws Exception {
        var doc=document(mapper);doc.put("createdBy","attacker");assertFalse(validator.validate(doc,false).valid());doc.remove("createdBy");
        doc.set("technical",mapper.createArrayNode());assertFalse(validator.validate(doc,false).valid());assertFalse(validator.validate(mapper.createArrayNode(),false).valid());
    }
    @Test void validatesImagesAsOneOrderedGalleryWithoutTrustingUploadMetadata() throws Exception {
        var doc=document(mapper);doc.set("media",mapper.readTree("[{\"url\":\"https://example.com/a.png\",\"alt\":\"Frente\",\"order\":1,\"primary\":true,\"file\":{\"supabasePath\":\"arbitrary\"}}]"));
        var result=validator.validate(doc,false);assertTrue(result.valid());assertTrue(result.document().path("media").get(0).path("file").isNull());
        ((ObjectNode)doc.path("media").get(0)).put("primary",false);assertFalse(validator.validate(doc,false).valid());
    }
    @Test void rejectsLocalFilesDuplicateImagesAndExcessiveImages() throws Exception {
        var doc=document(mapper);doc.set("media",mapper.readTree("[{\"url\":null,\"file\":{\"name\":\"x.png\"},\"order\":1,\"primary\":true}]"));assertFalse(validator.validate(doc,false).valid());
        var images=mapper.createArrayNode();for(int i=0;i<9;i++)images.addObject().put("url","https://example.com/a.png#"+i).put("order",i+1).put("primary",i==0);
        doc.set("media",images);assertFalse(validator.validate(doc,false).valid());images.remove(8);assertFalse(validator.validate(doc,false).valid());
    }
    @Test void restrictsRelatedRowsAndMonetaryPrecision() throws Exception {
        var doc=document(mapper);var rows=mapper.createArrayNode();for(int i=0;i<21;i++)rows.addObject();((ObjectNode)doc.get("relatedRecords")).set("memory",rows);
        assertFalse(validator.validate(doc,false).valid());doc.set("relatedRecords",mapper.createObjectNode());((ObjectNode)doc.get("product")).put("price",1.123);
        assertFalse(validator.validate(doc,false).valid());
    }
    @Test void allTenFrontendTypesAreSupportedAsUnverifiedDrafts() throws Exception {
        for(String type:List.of("cpu","board","ram","gpu","cooling","psu","case","storage","peripheral","other")){
            var doc=document(mapper);((ObjectNode)doc.get("product")).put("type",type);doc.put("requestedUse","catalog");doc.set("technical",mapper.createObjectNode());
            assertTrue(validator.validate(doc,false).valid(),type);
        }
    }
}
