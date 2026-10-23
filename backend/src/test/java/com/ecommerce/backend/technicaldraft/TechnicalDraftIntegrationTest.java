package com.ecommerce.backend.technicaldraft;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.technical-drafts.enabled=true", "spring.datasource.url=jdbc:h2:mem:technicaldrafttest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password="})
@AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
@Sql("/technical-drafts-test.sql")
class TechnicalDraftIntegrationTest {
    @Autowired TechnicalDraftService service;
    @Autowired TechnicalDraftRepository drafts;
    @Autowired TechnicalDraftEventRepository events;
    @Autowired ObjectMapper mapper;
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    static final UUID AUTHOR=UUID.fromString("10000000-1000-4000-8000-000000000001"),REVIEWER=UUID.fromString("10000000-1000-4000-8000-000000000002");
    private Jwt admin(UUID id){return Jwt.withTokenValue("test").header("alg","RS256").subject(id.toString()).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).claim("app_metadata",Map.of("role","admin")).build();}
    private ObjectNode document() throws Exception {return TechnicalDraftValidatorTest.document(mapper);}
    private TechnicalDraftDtos.Detail create() throws Exception {return service.create(admin(AUTHOR),document());}
    private void fault(int status,Runnable action){assertEquals(status,assertThrows(DraftFault.class,action::run).status);}
    @Test void realProductLinkIsVersionedRecoverableAndCannotChangeSkuSilently() throws Exception {
        UUID category=UUID.randomUUID(),type=UUID.randomUUID(),product=UUID.randomUUID();
        jdbc.update("insert into rya.categorias(id,nombre,slug) values (?,?,?)",category,"Procesadores","procesadores");
        jdbc.update("insert into rya.tipos_producto(id,codigo) values (?,?)",type,"HARDWARE");
        jdbc.update("insert into rya.productos(id,categoria_id,tipo_id,sku,nombre,slug,precio_base) values (?,?,?,?,?,?,?)",product,category,type,"TEST-CPU","CPU real","test-cpu",100);
        var created=create();var linked=service.link(admin(AUTHOR),created.id(),created.version(),false);
        assertEquals(1,linked.version());assertEquals(product.toString(),linked.document().path("catalogProductId").asText());
        assertEquals(product.toString(),service.revision(admin(AUTHOR),created.id(),1L).document().path("catalogProductId").asText());
        fault(409,()->service.link(admin(AUTHOR),created.id(),0L,false));
        fault(403,()->service.link(admin(REVIEWER),created.id(),linked.version(),true));
        var modified=linked.document().deepCopy();((ObjectNode)modified.get("product")).put("sku","OTHER");
        fault(409,()->service.update(admin(AUTHOR),created.id(),new TechnicalDraftDtos.Update(linked.version(),modified)));
        var unlinked=service.link(admin(AUTHOR),created.id(),linked.version(),true);assertFalse(unlinked.document().has("catalogProductId"));
        assertEquals(3,service.history(admin(AUTHOR),created.id(),0,10).getTotalElements());
        assertEquals(100,jdbc.queryForObject("select precio_base from rya.productos where id=?",Integer.class,product));
    }
    @Test void savesRecoversAndListsDraftsWithoutPublishingOrChangingInventory() throws Exception {
        long products=jdbc.queryForObject("SELECT count(*) FROM rya.productos",Long.class),stock=jdbc.queryForObject("SELECT count(*) FROM rya.existencias",Long.class);
        var created=create();assertEquals(0,created.version());assertEquals("DRAFT",created.state());assertEquals("pending",created.verificationStatus());
        assertEquals("TEST-CPU",service.get(admin(REVIEWER),created.id()).document().path("product").path("sku").asText());
        assertTrue(service.list(admin(AUTHOR),0,10,"TEST-CPU","DRAFT").getContent().stream().anyMatch(row->row.id().equals(created.id())));
        assertEquals(products,jdbc.queryForObject("SELECT count(*) FROM rya.productos",Long.class));assertEquals(stock,jdbc.queryForObject("SELECT count(*) FROM rya.existencias",Long.class));
    }
    @Test void recordsImmutableDocumentVersionsAndRejectsStaleUpdates() throws Exception {
        var created=create();var document=document();((ObjectNode)document.get("product")).put("name","New name");
        var updated=service.update(admin(AUTHOR),created.id(),new TechnicalDraftDtos.Update(created.version(),document));assertEquals(1,updated.version());
        fault(409,()->service.update(admin(AUTHOR),created.id(),new TechnicalDraftDtos.Update(created.version(),document)));
        assertEquals("CPU test",service.revision(admin(AUTHOR),created.id(),0L).document().path("product").path("name").asText());
        assertEquals(2,service.history(admin(AUTHOR),created.id(),0,10).getTotalElements());
    }
    @Test void requiresIndependentReviewAndKeepsApprovalSeparateFromCompatibility() throws Exception {
        var created=create();var submitted=service.submit(admin(AUTHOR),created.id(),created.version());assertEquals("IN_REVIEW",submitted.state());
        fault(403,()->service.decide(admin(AUTHOR),created.id(),new TechnicalDraftDtos.Decision(submitted.version(),"APPROVED","Reviewed")));
        fault(409,()->service.update(admin(AUTHOR),created.id(),new TechnicalDraftDtos.Update(submitted.version(),submitted.document())));
        var approved=service.decide(admin(REVIEWER),created.id(),new TechnicalDraftDtos.Decision(submitted.version(),"APPROVED","Quality reviewed; compatibility pending"));
        assertEquals("APPROVED",approved.state());assertEquals("pending",approved.document().path("verificationStatus").asText());
        var reopened=service.reopen(admin(AUTHOR),created.id(),new TechnicalDraftDtos.Transition(approved.version(),"Corrección"));assertEquals("DRAFT",reopened.state());
        assertEquals(4,service.history(admin(AUTHOR),created.id(),0,10).getTotalElements());
    }
    @Test void onlyAuthorCanModifyOrSubmitADraft() throws Exception {
        var created=create();fault(403,()->service.update(admin(REVIEWER),created.id(),new TechnicalDraftDtos.Update(created.version(),created.document())));
        fault(403,()->service.submit(admin(REVIEWER),created.id(),created.version()));
    }
    @Test void incompleteEvidenceBlocksSubmissionWithoutChangingState() throws Exception {
        var doc=document();((ObjectNode)doc.get("evidence")).putNull("date");var created=service.create(admin(AUTHOR),doc);
        fault(400,()->service.submit(admin(AUTHOR),created.id(),created.version()));assertEquals("DRAFT",service.get(admin(AUTHOR),created.id()).state());
    }
    @Test void malformedFiltersAndUnknownIdsDoNotLeakDatabaseErrors() {
        fault(400,()->service.list(admin(AUTHOR),-1,10,null,null));fault(400,()->service.list(admin(AUTHOR),0,51,null,null));
        fault(400,()->service.list(admin(AUTHOR),0,10,null,"PUBLISHED"));fault(404,()->service.get(admin(AUTHOR),UUID.randomUUID()));
    }
    @Test void anonymousAndUntrustedRolesCannotUseTheNewEndpoints() throws Exception {
        mvc.perform(get("/api/v1/admin/borradores-tecnicos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/borradores-tecnicos").with(jwt().jwt(token->token.subject(AUTHOR.toString()).claim("user_metadata",Map.of("role","admin")))))
                .andExpect(status().isForbidden());
    }
    @Test void authenticatedAdminCanCreateAndRetrieveThroughHttp() throws Exception {
        var response=mvc.perform(post("/api/v1/admin/borradores-tecnicos").with(jwt().jwt(token->token.subject(AUTHOR.toString()).claim("app_metadata",Map.of("role","admin"))))
                .contentType("application/json").content(mapper.writeValueAsString(new TechnicalDraftDtos.Create(document()))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.state").value("DRAFT")).andReturn();
        String id=mapper.readTree(response.getResponse().getContentAsString()).path("id").asText();
        mvc.perform(get("/api/v1/admin/borradores-tecnicos/"+id).with(jwt().jwt(token->token.subject(AUTHOR.toString()).claim("app_metadata",Map.of("role","admin")))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.document.technical.igpu").value(false));
    }
    @Test void doesNotCoerceFractionalVersionsOrAllowReviewWithoutReason() throws Exception {
        var created=create();var auth=jwt().jwt(token->token.subject(AUTHOR.toString()).claim("app_metadata",Map.of("role","admin")));
        mvc.perform(post("/api/v1/admin/borradores-tecnicos/"+created.id()+"/revision").with(auth).contentType("application/json").content("{\"version\":0.9}"))
                .andExpect(status().isBadRequest());
        var submitted=service.submit(admin(AUTHOR),created.id(),created.version());
        fault(400,()->service.decide(admin(REVIEWER),created.id(),new TechnicalDraftDtos.Decision(submitted.version(),"APPROVED"," ")));
        assertEquals("IN_REVIEW",service.get(admin(REVIEWER),created.id()).state());
    }
    @Test void requestLimitsAndValidationErrorsAreExplicit() throws Exception {
        var auth=jwt().jwt(token->token.subject(AUTHOR.toString()).claim("app_metadata",Map.of("role","admin")));
        mvc.perform(post("/api/v1/admin/borradores-tecnicos").with(auth).contentType("application/json").content("{\"document\":\""+"x".repeat(131072)+"\"}"))
                .andExpect(status().isPayloadTooLarge());
        mvc.perform(post("/api/v1/admin/borradores-tecnicos").with(auth).contentType("application/json").content("{invalid"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/admin/borradores-tecnicos").with(auth).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }
}
