package handoff.application.Controller;

import com.jayway.jsonpath.JsonPath;
import handoff.application.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RequestFlowTests extends ApiTestSupport {

    private String auth;
    private String projectId;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        auth = registerOrganization("Org Solicitações");
        String clientId = createClient(auth, "Padaria Central");
        projectId = createProject(auth, clientId, "Site institucional");
        token = createPortalLink(auth, projectId);
    }

    @Test
    void fullFlowBetweenClientAndDeveloper() throws Exception {
        String body = portalPost(token, "/requests", """
                {"title":"Trocar a foto da home","description":"Usar a foto nova da fachada","priority":"HIGH"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number", is(1)))
                .andExpect(jsonPath("$.code", is("#001")))
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.createdByType", is("CLIENT")))
                .andExpect(jsonPath("$.priority", is("HIGH")))
                .andReturn().getResponse().getContentAsString();
        String requestId = JsonPath.read(body, "$.id");

        getJson(auth, "/api/app/projects/" + projectId + "/requests").andExpect(jsonPath("$.items", hasSize(1)));

        changeStatus(requestId, "IN_PROGRESS").andExpect(status().isOk());
        changeStatus(requestId, "AWAITING_APPROVAL").andExpect(status().isOk());

        portalPost(token, "/requests/" + requestId + "/request-changes", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CHANGES_REQUESTED")));

        changeStatus(requestId, "IN_PROGRESS").andExpect(status().isOk());
        changeStatus(requestId, "AWAITING_APPROVAL").andExpect(status().isOk());

        portalPost(token, "/requests/" + requestId + "/approve", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DONE")));

        portalGet(token, "/requests/" + requestId).andExpect(jsonPath("$.status", is("DONE")));
    }

    @Test
    void numbersAreSequentialPerProject() throws Exception {
        createRequest(auth, projectId, "Primeira");
        portalPost(token, "/requests", """
                {"title":"Segunda"}
                """).andExpect(jsonPath("$.number", is(2)));

        String otherProject = createProject(auth, createClient(auth, "Outro cliente"), "Outro projeto");
        postJson(auth, "/api/app/projects/" + otherProject + "/requests", """
                {"title":"Primeira do outro"}
                """).andExpect(jsonPath("$.number", is(1)));
    }

    @Test
    void concurrentCreationNeverRepeatsNumbers() throws Exception {
        int total = 10;
        ExecutorService executor = Executors.newFixedThreadPool(total);
        try {
            List<Callable<Integer>> tasks = new ArrayList<>();
            for (int i = 0; i < total; i++) {
                int index = i;
                tasks.add(() -> {
                    String body = postJson(auth, "/api/app/projects/" + projectId + "/requests", """
                            {"title":"Paralela %d"}
                            """.formatted(index))
                            .andExpect(status().isCreated())
                            .andReturn().getResponse().getContentAsString();
                    return JsonPath.read(body, "$.number");
                });
            }
            Set<Integer> numbers = new java.util.HashSet<>();
            for (Future<Integer> future : executor.invokeAll(tasks)) {
                numbers.add(future.get());
            }

            assertThat(numbers).isEqualTo(IntStream.rangeClosed(1, total).boxed().collect(Collectors.toSet()));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void clientCannotDoDeveloperTransitions() throws Exception {
        String requestId = createRequest(auth, projectId, "Ajuste");

        portalPost(token, "/requests/" + requestId + "/approve", "").andExpect(status().isConflict());

        changeStatus(requestId, "IN_PROGRESS");
        changeStatus(requestId, "AWAITING_APPROVAL");
        changeStatus(requestId, "DONE").andExpect(status().isForbidden());
        changeStatus(requestId, "CHANGES_REQUESTED").andExpect(status().isForbidden());
    }

    @Test
    void invalidTransitionsReturn409() throws Exception {
        String requestId = createRequest(auth, projectId, "Ajuste");

        changeStatus(requestId, "AWAITING_APPROVAL").andExpect(status().isConflict());
        changeStatus(requestId, "CANCELED").andExpect(status().isOk());
        changeStatus(requestId, "IN_PROGRESS").andExpect(status().isConflict());
    }

    @Test
    void developerCanAssignSomeoneFromTheSameOrganization() throws Exception {
        String requestId = createRequest(auth, projectId, "Ajuste");
        String meBody = getJson(auth, "/api/app/me").andReturn().getResponse().getContentAsString();
        String myId = JsonPath.read(meBody, "$.id");

        putJson(auth, "/api/app/requests/" + requestId + "/assignment", """
                {"assigneeId":"%s","dueDate":"2026-12-20"}
                """.formatted(myId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId", is(myId)))
                .andExpect(jsonPath("$.dueDate", is("2026-12-20")));

        String otherAuth = registerOrganization("Outra org");
        String otherId = JsonPath.read(getJson(otherAuth, "/api/app/me").andReturn().getResponse().getContentAsString(), "$.id");
        putJson(auth, "/api/app/requests/" + requestId + "/assignment", """
                {"assigneeId":"%s"}
                """.formatted(otherId))
                .andExpect(status().isNotFound());
    }

    @Test
    void portalLinkOnlySeesItsOwnProject() throws Exception {
        String otherProject = createProject(auth, createClient(auth, "Academia Fit"), "App de treinos");
        String requestOfOtherProject = createRequest(auth, otherProject, "Pedido da academia");
        createRequest(auth, projectId, "Pedido da padaria");

        portalGet(token, "/requests")
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title", is("Pedido da padaria")));
        portalGet(token, "/requests/" + requestOfOtherProject).andExpect(status().isNotFound());
        portalPost(token, "/requests/" + requestOfOtherProject + "/approve", "").andExpect(status().isNotFound());
    }

    @Test
    void anotherOrganizationCannotTouchRequests() throws Exception {
        String requestId = createRequest(auth, projectId, "Pedido");
        String otherAuth = registerOrganization("Org Intrusa");

        getJson(otherAuth, "/api/app/requests/" + requestId).andExpect(status().isNotFound());
        getJson(otherAuth, "/api/app/projects/" + projectId + "/requests").andExpect(status().isNotFound());
        patchStatus(otherAuth, requestId, "IN_PROGRESS").andExpect(status().isNotFound());
        postJson(otherAuth, "/api/app/projects/" + projectId + "/requests", """
                {"title":"Invasão"}
                """).andExpect(status().isNotFound());

        getJson(auth, "/api/app/requests/" + requestId).andExpect(jsonPath("$.status", is("OPEN")));
    }

    private org.springframework.test.web.servlet.ResultActions changeStatus(String requestId, String status) throws Exception {
        return patchStatus(auth, requestId, status);
    }

    private org.springframework.test.web.servlet.ResultActions patchStatus(String authHeader, String requestId, String status) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/app/requests/" + requestId + "/status")
                .header("Authorization", authHeader)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"%s"}
                        """.formatted(status)));
    }
}
