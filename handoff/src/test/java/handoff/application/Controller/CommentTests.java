package handoff.application.Controller;

import handoff.application.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentTests extends ApiTestSupport {

    private String auth;
    private String projectId;
    private String token;
    private String requestId;

    @BeforeEach
    void setUp() throws Exception {
        auth = registerOrganization("Org Comentários");
        String clientId = createClient(auth, "Padaria Central");
        projectId = createProject(auth, clientId, "Site institucional");
        token = createPortalLink(auth, projectId);
        requestId = createRequest(auth, projectId, "Trocar a foto da home");
    }

    @Test
    void developerAndClientTalkInsideTheRequest() throws Exception {
        portalPost(token, "/requests/" + requestId + "/comments", """
                {"body":"Pode usar a foto da fachada nova?"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorType", is("CLIENT")))
                .andExpect(jsonPath("$.authorName", is("Padaria Central")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        postJson(auth, "/api/app/requests/" + requestId + "/comments", """
                {"body":"Claro, subo hoje à tarde."}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorType", is("USER")))
                .andExpect(jsonPath("$.authorName", is("Dono")));

        getJson(auth, "/api/app/requests/" + requestId + "/comments")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].body", is("Pode usar a foto da fachada nova?")))
                .andExpect(jsonPath("$[1].body", is("Claro, subo hoje à tarde.")));

        portalGet(token, "/requests/" + requestId + "/comments")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].authorType", is("USER")));
    }

    @Test
    void emptyCommentReturns400() throws Exception {
        postJson(auth, "/api/app/requests/" + requestId + "/comments", """
                {"body":"   "}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.body", notNullValue()));
        portalPost(token, "/requests/" + requestId + "/comments", "{}").andExpect(status().isBadRequest());
    }

    @Test
    void portalCannotCommentOnAnotherProject() throws Exception {
        String otherProject = createProject(auth, createClient(auth, "Academia Fit"), "App de treinos");
        String otherRequest = createRequest(auth, otherProject, "Pedido da academia");

        portalPost(token, "/requests/" + otherRequest + "/comments", """
                {"body":"Comentário fora do meu projeto"}
                """).andExpect(status().isNotFound());
        portalGet(token, "/requests/" + otherRequest + "/comments").andExpect(status().isNotFound());

        getJson(auth, "/api/app/requests/" + otherRequest + "/comments").andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void anotherOrganizationCannotReadOrWriteComments() throws Exception {
        postJson(auth, "/api/app/requests/" + requestId + "/comments", """
                {"body":"Comentário interno"}
                """).andExpect(status().isCreated());
        String otherAuth = registerOrganization("Org Intrusa");

        getJson(otherAuth, "/api/app/requests/" + requestId + "/comments").andExpect(status().isNotFound());
        postJson(otherAuth, "/api/app/requests/" + requestId + "/comments", """
                {"body":"Invasão"}
                """).andExpect(status().isNotFound());

        getJson(auth, "/api/app/requests/" + requestId + "/comments").andExpect(jsonPath("$", hasSize(1)));
    }
}
