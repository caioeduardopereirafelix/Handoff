package handoff.application.Controller;

import handoff.application.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaginationTests extends ApiTestSupport {

    private String auth;
    private String projectId;
    private String requestsUrl;

    @BeforeEach
    void setUp() throws Exception {
        auth = registerOrganization("Org Paginação");
        projectId = createProject(auth, createClient(auth, "Padaria"), "Site");
        requestsUrl = "/api/app/projects/" + projectId + "/requests";
        createRequest(auth, projectId, "Primeiro");
        String second = createRequest(auth, projectId, "Segundo");
        createRequest(auth, projectId, "Terceiro");
        mvc.perform(patch("/api/app/requests/" + second + "/status")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void requestsComeInPagesNewestFirst() throws Exception {
        getJson(auth, requestsUrl + "?size=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].title", is("Terceiro")))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.totalItems", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)));

        getJson(auth, requestsUrl + "?size=2&page=1")
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title", is("Primeiro")));
    }

    @Test
    void pageSizeIsCappedAt100AndDefaultsTo50() throws Exception {
        getJson(auth, requestsUrl + "?size=100000").andExpect(jsonPath("$.size", is(100)));
        getJson(auth, requestsUrl).andExpect(jsonPath("$.size", is(50)));
        getJson(auth, requestsUrl + "?size=0&page=-3")
                .andExpect(jsonPath("$.size", is(1)))
                .andExpect(jsonPath("$.page", is(0)));
    }

    @Test
    void requestsCanBeFilteredByStatus() throws Exception {
        getJson(auth, requestsUrl + "?status=OPEN")
                .andExpect(jsonPath("$.totalItems", is(2)));
        getJson(auth, requestsUrl + "?status=IN_PROGRESS&status=DONE")
                .andExpect(jsonPath("$.totalItems", is(1)))
                .andExpect(jsonPath("$.items[0].title", is("Segundo")));
    }

    @Test
    void portalListIsPagedAndFilteredToo() throws Exception {
        String token = createPortalLink(auth, projectId);

        portalGet(token, "/requests?status=IN_PROGRESS&size=1")
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.totalItems", is(1)));
        portalGet(token, "/requests?size=1")
                .andExpect(jsonPath("$.totalPages", is(3)));
    }

    @Test
    void clientsAndProjectsArePaged() throws Exception {
        createClient(auth, "Academia");

        getJson(auth, "/api/app/clients?size=1")
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].name", is("Academia")))
                .andExpect(jsonPath("$.totalItems", is(2)));
        getJson(auth, "/api/app/projects?size=1")
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.totalItems", is(1)));
    }

    @Test
    void invalidStatusIsRejected() throws Exception {
        getJson(auth, requestsUrl + "?status=INVENTADO").andExpect(status().isBadRequest());
    }
}
