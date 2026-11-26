package handoff.application.Controller;

import com.jayway.jsonpath.JsonPath;
import handoff.application.ApiTestSupport;
import handoff.application.Config.TenantContext;
import handoff.application.Entity.Invitation;
import handoff.application.Repository.InvitationRepository;
import handoff.application.Service.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InvitationTests extends ApiTestSupport {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private TenantContext tenantContext;

    @Value("${mailpit.api-url}")
    private String mailpitApi;

    private String ownerAuth;

    @BeforeEach
    void setUp() throws Exception {
        ownerAuth = registerOrganization("Estúdio Convite");
    }

    @Test
    void invitedDeveloperJoinsTheSameOrganizationAsMemberAndSeesItsProjects() throws Exception {
        createProject(ownerAuth, createClient(ownerAuth, "Padaria"), "Site");
        String email = uniqueEmail();
        String token = invite(email).andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", is("MEMBER")))
                .andExpect(jsonPath("$.token", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        token = JsonPath.read(token, "$.token");

        mvc.perform(get("/api/invitations/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationName", is("Estúdio Convite")))
                .andExpect(jsonPath("$.invitedByName", is("Dono")))
                .andExpect(jsonPath("$.email", is(email)));

        String body = accept(token, "Bia Dev").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String memberAuth = "Bearer " + JsonPath.read(body, "$.token");

        getJson(memberAuth, "/api/app/me")
                .andExpect(jsonPath("$.role", is("MEMBER")))
                .andExpect(jsonPath("$.name", is("Bia Dev")))
                .andExpect(jsonPath("$.organizationName", is("Estúdio Convite")));
        getJson(memberAuth, "/api/app/projects").andExpect(jsonPath("$.items", hasSize(1)));
        login(email, PASSWORD);
    }

    @Test
    void invitationCanBeUsedOnlyOnce() throws Exception {
        String token = tokenOf(invite(uniqueEmail()));
        accept(token, "Primeiro").andExpect(status().isOk());

        accept(token, "Segundo").andExpect(status().isNotFound());
        mvc.perform(get("/api/invitations/" + token)).andExpect(status().isNotFound());
    }

    @Test
    void expiredInvitationIsRejected() throws Exception {
        String token = tokenOf(invite(uniqueEmail()));
        tenantContext.acrossTenants(() -> {
            Invitation invitation = invitationRepository.findByTokenHash(TokenHasher.sha256Hex(token)).orElseThrow();
            invitation.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
            return invitationRepository.saveAndFlush(invitation);
        });

        mvc.perform(get("/api/invitations/" + token)).andExpect(status().isNotFound());
        accept(token, "Atrasado").andExpect(status().isNotFound());
    }

    @Test
    void revokedInvitationIsRejected() throws Exception {
        String body = invite(uniqueEmail()).andReturn().getResponse().getContentAsString();

        deleteJson(ownerAuth, "/api/app/team/invitations/" + JsonPath.read(body, "$.id")).andExpect(status().isNoContent());

        accept(JsonPath.read(body, "$.token"), "Cancelado").andExpect(status().isNotFound());
    }

    @Test
    void invitingTheSameEmailAgainReplacesThePreviousInvitation() throws Exception {
        String email = uniqueEmail();
        String first = tokenOf(invite(email));
        String second = tokenOf(invite(email));

        accept(first, "Antigo").andExpect(status().isNotFound());
        accept(second, "Novo").andExpect(status().isOk());
    }

    @Test
    void emailThatAlreadyHasAnAccountCannotBeInvited() throws Exception {
        String email = uniqueEmail();
        accept(tokenOf(invite(email)), "Já tem conta").andExpect(status().isOk());

        invite(email).andExpect(status().isConflict());
    }

    @Test
    void onlyOwnersInvite() throws Exception {
        String body = accept(tokenOf(invite(uniqueEmail())), "Membro").andReturn().getResponse().getContentAsString();
        String memberAuth = "Bearer " + JsonPath.read(body, "$.token");

        postJson(memberAuth, "/api/app/team/invitations", """
                {"email":"%s"}
                """.formatted(uniqueEmail())).andExpect(status().isForbidden());
    }

    @Test
    void anotherOrganizationCannotRevokeTheInvitation() throws Exception {
        String body = invite(uniqueEmail()).andReturn().getResponse().getContentAsString();
        String intruder = registerOrganization("Org Intrusa");

        deleteJson(intruder, "/api/app/team/invitations/" + JsonPath.read(body, "$.id")).andExpect(status().isNotFound());
        accept(JsonPath.read(body, "$.token"), "Ainda vale").andExpect(status().isOk());
    }

    @Test
    void acceptValidatesNameAndPassword() throws Exception {
        String token = tokenOf(invite(uniqueEmail()));

        mvc.perform(post("/api/invitations/" + token + "/accept").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","password":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name", notNullValue()))
                .andExpect(jsonPath("$.errors.password", notNullValue()));
        accept(token, "Depois certo").andExpect(status().isOk());
    }

    @Test
    void unknownTokenReturns404() throws Exception {
        mvc.perform(get("/api/invitations/" + TokenHasher.newToken())).andExpect(status().isNotFound());
    }

    @Test
    void invitationEmailCarriesTheLink() throws Exception {
        String email = uniqueEmail();
        String token = tokenOf(invite(email));

        Map<String, Object> message = await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(300))
                .until(() -> findMessage(email), found -> found != null);

        assertThat((String) message.get("Subject")).contains("Estúdio Convite");
        assertThat(messageText(message)).contains("/convite/" + token);
    }

    private ResultActions invite(String email) throws Exception {
        return postJson(ownerAuth, "/api/app/team/invitations", """
                {"email":"%s"}
                """.formatted(email));
    }

    private ResultActions accept(String token, String name) throws Exception {
        return mvc.perform(post("/api/invitations/" + token + "/accept").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"%s","password":"%s"}
                        """.formatted(name, PASSWORD)));
    }

    private static String tokenOf(ResultActions created) throws Exception {
        return JsonPath.read(created.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.token");
    }

    private Map<String, Object> findMessage(String recipient) throws Exception {
        String query = URLEncoder.encode("to:\"" + recipient + "\"", StandardCharsets.UTF_8);
        String body = HTTP.send(HttpRequest.newBuilder(URI.create(mailpitApi + "/search?query=" + query)).GET().build(),
                HttpResponse.BodyHandlers.ofString()).body();
        List<Map<String, Object>> messages = JsonPath.read(body, "$.messages");
        return messages.stream().findFirst().orElse(null);
    }

    private String messageText(Map<String, Object> message) throws Exception {
        String body = HTTP.send(HttpRequest.newBuilder(URI.create(mailpitApi + "/message/" + message.get("ID"))).GET().build(),
                HttpResponse.BodyHandlers.ofString()).body();
        return JsonPath.read(body, "$.Text");
    }
}
