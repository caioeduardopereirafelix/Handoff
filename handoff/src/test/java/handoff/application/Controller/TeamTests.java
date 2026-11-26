package handoff.application.Controller;

import com.jayway.jsonpath.JsonPath;
import handoff.application.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeamTests extends ApiTestSupport {

    private String ownerAuth;
    private String ownerId;

    @BeforeEach
    void setUp() throws Exception {
        ownerAuth = registerOrganization("Estúdio Equipe");
        ownerId = JsonPath.read(getJson(ownerAuth, "/api/app/me").andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Test
    void teamListsMembersAndPendingInvitations() throws Exception {
        Member bia = join("Bia");
        String pendingEmail = uniqueEmail();
        invite(pendingEmail);
        String revoked = JsonPath.read(invite(uniqueEmail()), "$.id");
        deleteJson(ownerAuth, "/api/app/team/invitations/" + revoked).andExpect(status().isNoContent());

        getJson(bia.auth(), "/api/app/team")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(2)))
                .andExpect(jsonPath("$.members[*].name", containsInAnyOrder("Dono", "Bia")))
                .andExpect(jsonPath("$.invitations", hasSize(1)))
                .andExpect(jsonPath("$.invitations[0].email", is(pendingEmail)))
                .andExpect(jsonPath("$.invitations[0].invitedByName", is("Dono")));
    }

    @Test
    void deactivatedMemberLosesAccessImmediately() throws Exception {
        Member bia = join("Bia");

        update(bia.id(), "{\"active\":false}").andExpect(status().isOk()).andExpect(jsonPath("$.active", is(false)));

        getJson(bia.auth(), "/api/app/me").andExpect(status().isUnauthorized());
        refresh(bia.refreshToken()).andExpect(status().isUnauthorized());
        attemptLogin(bia.email()).andExpect(status().isUnauthorized());

        update(bia.id(), "{\"active\":true}").andExpect(status().isOk());
        attemptLogin(bia.email()).andExpect(status().isOk());
    }

    @Test
    void promotedMemberCanInviteAndDemotedOwnerCannot() throws Exception {
        Member bia = join("Bia");

        update(bia.id(), "{\"role\":\"OWNER\"}").andExpect(status().isOk()).andExpect(jsonPath("$.role", is("OWNER")));
        postJson(bia.auth(), "/api/app/team/invitations", "{\"email\":\"%s\"}".formatted(uniqueEmail()))
                .andExpect(status().isCreated());

        update(bia.id(), "{\"role\":\"MEMBER\"}").andExpect(status().isOk());
        postJson(bia.auth(), "/api/app/team/invitations", "{\"email\":\"%s\"}".formatted(uniqueEmail()))
                .andExpect(status().isForbidden());
    }

    @Test
    void nobodyChangesTheirOwnAccess() throws Exception {
        update(ownerId, "{\"active\":false}").andExpect(status().isConflict());
        update(ownerId, "{\"role\":\"MEMBER\"}").andExpect(status().isConflict());
    }

    @Test
    void theLastActiveOwnerCannotBeRemoved() throws Exception {
        Member bia = join("Bia");
        update(bia.id(), "{\"role\":\"OWNER\"}").andExpect(status().isOk());

        mvc.perform(patch("/api/app/team/members/" + ownerId).header("Authorization", bia.auth())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"MEMBER\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/app/team/members/" + ownerId).header("Authorization", bia.auth())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"OWNER\"}"))
                .andExpect(status().isOk());

        update(bia.id(), "{\"active\":false}").andExpect(status().isOk());
        mvc.perform(patch("/api/app/team/members/" + ownerId).header("Authorization", ownerAuth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"MEMBER\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void membersCannotChangeTheTeam() throws Exception {
        Member bia = join("Bia");
        Member caio = join("Caio");

        mvc.perform(patch("/api/app/team/members/" + caio.id()).header("Authorization", bia.auth())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anotherOrganizationCannotSeeOrChangeTheMember() throws Exception {
        Member bia = join("Bia");
        String intruder = registerOrganization("Org Intrusa");

        getJson(intruder, "/api/app/team").andExpect(jsonPath("$.members", hasSize(1)));
        mvc.perform(patch("/api/app/team/members/" + bia.id()).header("Authorization", intruder)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isNotFound());
        update(UUID.randomUUID().toString(), "{\"active\":false}").andExpect(status().isNotFound());
    }

    private record Member(String id, String email, String auth, String refreshToken) {
    }

    private Member join(String name) throws Exception {
        String email = uniqueEmail();
        String token = JsonPath.read(invite(email), "$.token");
        String body = mvc.perform(post("/api/invitations/" + token + "/accept").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"password\":\"%s\"}".formatted(name, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String auth = "Bearer " + JsonPath.read(body, "$.token");
        String id = JsonPath.read(getJson(auth, "/api/app/me").andReturn().getResponse().getContentAsString(), "$.id");
        return new Member(id, email, auth, JsonPath.read(body, "$.refreshToken"));
    }

    private String invite(String email) throws Exception {
        return postJson(ownerAuth, "/api/app/team/invitations", "{\"email\":\"%s\"}".formatted(email))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private ResultActions update(String memberId, String json) throws Exception {
        return mvc.perform(patch("/api/app/team/members/" + memberId).header("Authorization", ownerAuth)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)));
    }

    private ResultActions attemptLogin(String email) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)));
    }
}
