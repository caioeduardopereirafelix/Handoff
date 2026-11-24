package handoff.application.Controller;

import com.jayway.jsonpath.JsonPath;
import handoff.application.ApiTestSupport;
import handoff.application.Config.TenantContext;
import handoff.application.Entity.PortalAccess;
import handoff.application.Repository.PortalAccessRepository;
import handoff.application.Service.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PortalAccessTests extends ApiTestSupport {

    @Autowired
    private PortalAccessRepository portalAccessRepository;

    @Autowired
    private TenantContext tenantContext;

    private String auth;
    private String projectId;

    @BeforeEach
    void setUp() throws Exception {
        auth = registerOrganization("Org Portal");
        String clientId = createClient(auth, "Padaria Central");
        projectId = createProject(auth, clientId, "Site institucional");
    }

    @Test
    void createdLinkOpensTheProjectInThePortal() throws Exception {
        String body = postJson(auth, "/api/app/projects/" + projectId + "/portal-access", """
                {"expiresInDays":30}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.expiresAt", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");

        portalGet(token, "/project")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectName", is("Site institucional")))
                .andExpect(jsonPath("$.clientName", is("Padaria Central")));
    }

    @Test
    void onlyTheHashIsStoredAndUsageIsRecorded() throws Exception {
        String token = createPortalLink(auth, projectId);

        PortalAccess stored = tenantContext.acrossTenants(
                () -> portalAccessRepository.findByTokenHash(TokenHasher.sha256Hex(token)).orElseThrow());
        assertThat(stored.getTokenHash()).isNotEqualTo(token).hasSize(64);
        assertThat(stored.getLastUsedAt()).isNull();

        portalGet(token, "/project").andExpect(status().isOk());

        PortalAccess used = tenantContext.acrossTenants(
                () -> portalAccessRepository.findById(stored.getId()).orElseThrow());
        assertThat(used.getLastUsedAt()).isNotNull();
        assertThat(used.getLastUsedIp()).isNotBlank();
    }

    @Test
    void listingLinksNeverReturnsTheToken() throws Exception {
        createPortalLink(auth, projectId);

        getJson(auth, "/api/app/projects/" + projectId + "/portal-access")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].active", is(true)))
                .andExpect(jsonPath("$[0].token").doesNotExist());
    }

    @Test
    void revokedLinkStopsWorkingImmediately() throws Exception {
        String token = createPortalLink(auth, projectId);
        String accessId = JsonPath.read(getJson(auth, "/api/app/projects/" + projectId + "/portal-access")
                .andReturn().getResponse().getContentAsString(), "$[0].id");

        deleteJson(auth, "/api/app/projects/" + projectId + "/portal-access/" + accessId)
                .andExpect(status().isNoContent());

        portalGet(token, "/project").andExpect(status().isNotFound());
        getJson(auth, "/api/app/projects/" + projectId + "/portal-access")
                .andExpect(jsonPath("$[0].active", is(false)))
                .andExpect(jsonPath("$[0].revokedAt", notNullValue()));
    }

    @Test
    void expiredLinkReturns404() throws Exception {
        String token = createPortalLink(auth, projectId);
        PortalAccess access = tenantContext.acrossTenants(
                () -> portalAccessRepository.findByTokenHash(TokenHasher.sha256Hex(token)).orElseThrow());
        access.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        tenantContext.acrossTenants(() -> portalAccessRepository.saveAndFlush(access));

        portalGet(token, "/project").andExpect(status().isNotFound());
    }

    @Test
    void unknownOrMissingTokenReturns404() throws Exception {
        portalGet(TokenHasher.newToken(), "/project").andExpect(status().isNotFound());
        portalGet("", "/project").andExpect(status().isNotFound());
    }

    @Test
    void developerJwtDoesNotOpenThePortal() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/portal/qualquer-coisa/project").header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void linkFromAnotherOrganizationCannotBeRevoked() throws Exception {
        createPortalLink(auth, projectId);
        String accessId = JsonPath.read(getJson(auth, "/api/app/projects/" + projectId + "/portal-access")
                .andReturn().getResponse().getContentAsString(), "$[0].id");

        String otherAuth = registerOrganization("Org Intrusa");
        deleteJson(otherAuth, "/api/app/projects/" + projectId + "/portal-access/" + accessId)
                .andExpect(status().isNotFound());
        postJson(otherAuth, "/api/app/projects/" + projectId + "/portal-access", "{}")
                .andExpect(status().isNotFound());

        getJson(auth, "/api/app/projects/" + projectId + "/portal-access")
                .andExpect(jsonPath("$[0].revokedAt", nullValue()));
    }
}
