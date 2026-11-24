package handoff.application.Controller;

import handoff.application.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActivityLogTests extends ApiTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String auth;
    private String projectId;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        auth = registerOrganization("Org Histórico");
        String clientId = createClient(auth, "Padaria Central");
        projectId = createProject(auth, clientId, "Site institucional");
        token = createPortalLink(auth, projectId);
    }

    @Test
    void everyStepOfTheFlowIsRecordedInOrder() throws Exception {
        String body = portalPost(token, "/requests", """
                {"title":"Trocar a foto da home"}
                """).andReturn().getResponse().getContentAsString();
        String requestId = com.jayway.jsonpath.JsonPath.read(body, "$.id");

        changeStatus(requestId, "IN_PROGRESS");
        postJson(auth, "/api/app/requests/" + requestId + "/comments", """
                {"body":"Começando agora"}
                """);
        changeStatus(requestId, "AWAITING_APPROVAL");
        portalPost(token, "/requests/" + requestId + "/approve", "");

        getJson(auth, "/api/app/requests/" + requestId + "/activity")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].action", is("REQUEST_CREATED")))
                .andExpect(jsonPath("$[0].actorType", is("CLIENT")))
                .andExpect(jsonPath("$[0].actorName", is("Padaria Central")))
                .andExpect(jsonPath("$[0].toStatus", is("OPEN")))
                .andExpect(jsonPath("$[1].action", is("STATUS_CHANGED")))
                .andExpect(jsonPath("$[1].actorType", is("USER")))
                .andExpect(jsonPath("$[1].actorName", is("Dono")))
                .andExpect(jsonPath("$[1].fromStatus", is("OPEN")))
                .andExpect(jsonPath("$[1].toStatus", is("IN_PROGRESS")))
                .andExpect(jsonPath("$[2].action", is("COMMENT_ADDED")))
                .andExpect(jsonPath("$[2].fromStatus", nullValue()))
                .andExpect(jsonPath("$[3].toStatus", is("AWAITING_APPROVAL")))
                .andExpect(jsonPath("$[4].actorType", is("CLIENT")))
                .andExpect(jsonPath("$[4].fromStatus", is("AWAITING_APPROVAL")))
                .andExpect(jsonPath("$[4].toStatus", is("DONE")));
    }

    @Test
    void clientSeesTheSameHistoryInThePortal() throws Exception {
        String requestId = createRequest(auth, projectId, "Ajuste");
        changeStatus(requestId, "IN_PROGRESS");

        portalGet(token, "/requests/" + requestId + "/activity")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].action", is("REQUEST_CREATED")))
                .andExpect(jsonPath("$[1].action", is("STATUS_CHANGED")));
    }

    @Test
    void failedTransitionAndAssignmentChangesAreHandled() throws Exception {
        String requestId = createRequest(auth, projectId, "Ajuste");

        changeStatus(requestId, "DONE").andExpect(status().isConflict());
        putJson(auth, "/api/app/requests/" + requestId + "/assignment", """
                {"dueDate":"2026-12-20"}
                """).andExpect(status().isOk());

        getJson(auth, "/api/app/requests/" + requestId + "/activity")
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].action", is("ASSIGNMENT_CHANGED")));
    }

    @Test
    void historyIsIsolatedByProjectAndOrganization() throws Exception {
        String otherProject = createProject(auth, createClient(auth, "Academia Fit"), "App de treinos");
        String otherRequest = createRequest(auth, otherProject, "Pedido da academia");
        String otherAuth = registerOrganization("Org Intrusa");

        portalGet(token, "/requests/" + otherRequest + "/activity").andExpect(status().isNotFound());
        getJson(otherAuth, "/api/app/requests/" + otherRequest + "/activity").andExpect(status().isNotFound());
    }

    @Test
    void databaseRejectsUpdatesToTheLog() throws Exception {
        String requestId = createRequest(auth, projectId, "Ajuste");
        UUID logId = jdbcTemplate.queryForObject(
                "select id from activity_log where request_id = ?::uuid", UUID.class, requestId);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "update activity_log set action = 'COMMENT_ADDED' where id = ?", logId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("somente inserção");

        assertThat(jdbcTemplate.queryForObject("select action from activity_log where id = ?", String.class, logId))
                .isEqualTo("REQUEST_CREATED");
    }

    private org.springframework.test.web.servlet.ResultActions changeStatus(String requestId, String status) throws Exception {
        return mvc.perform(patch("/api/app/requests/" + requestId + "/status")
                .header("Authorization", auth)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"%s"}
                        """.formatted(status)));
    }
}
