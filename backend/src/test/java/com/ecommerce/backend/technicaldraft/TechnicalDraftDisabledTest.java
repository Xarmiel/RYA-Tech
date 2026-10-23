package com.ecommerce.backend.technicaldraft;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TechnicalDraftDisabledTest {
    @Test void featureIsUnavailableUntilTheMigrationIsApprovedAndEnabled() {
        var drafts=mock(TechnicalDraftRepository.class);var events=mock(TechnicalDraftEventRepository.class);
        var mapper=new ObjectMapper();var service=new TechnicalDraftService(drafts,events,new TechnicalDraftValidator(mapper),mapper,mock(TechnicalDraftFicha.class),false);
        var admin=Jwt.withTokenValue("test").header("alg","RS256").subject(UUID.randomUUID().toString()).claim("app_metadata",Map.of("role","admin")).build();
        DraftFault error=assertThrows(DraftFault.class,()->service.list(admin,0,10,null,null));assertEquals(503,error.status);
        verifyNoInteractions(drafts,events);
    }
    @Test void serviceDoesNotTrustUserEditableMetadataEvenIfCalledOutsideTheController() {
        var drafts=mock(TechnicalDraftRepository.class);var events=mock(TechnicalDraftEventRepository.class);
        var mapper=new ObjectMapper();var service=new TechnicalDraftService(drafts,events,new TechnicalDraftValidator(mapper),mapper,mock(TechnicalDraftFicha.class),true);
        var user=Jwt.withTokenValue("test").header("alg","RS256").subject(UUID.randomUUID().toString()).claim("user_metadata",Map.of("role","admin")).build();
        assertEquals(403,assertThrows(DraftFault.class,()->service.list(user,0,10,null,null)).status);
        verifyNoInteractions(drafts,events);
    }
}
