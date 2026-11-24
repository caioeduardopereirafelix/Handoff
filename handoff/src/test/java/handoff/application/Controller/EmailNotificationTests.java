package handoff.application.Controller;

import com.jayway.jsonpath.JsonPath;
import handoff.application.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmailNotificationTests extends ApiTestSupport {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Value("${mailpit.api-url}")
    private String mailpitApi;

    private String ownerEmail;
    private String clientEmail;
    private String auth;
    private String projectId;

    @BeforeEach
    void setUp() throws Exception {
        ownerEmail = uniqueEmail();
        clientEmail = "cliente-" + UUID.randomUUID() + "@padaria.com";
        String body = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/auth/register").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"organizationName":"Estúdio Notificações","name":"Caio","email":"%s","password":"%s"}
                                """.formatted(ownerEmail, PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        auth = "Bearer " + JsonPath.read(body, "$.token");

        String clientBody = postJson(auth, "/api/app/clients", """
                {"name":"Padaria Central","contactName":"Ana","email":"%s"}
                """.formatted(clientEmail)).andReturn().getResponse().getContentAsString();
        projectId = createProject(auth, JsonPath.read(clientBody, "$.id"), "Site institucional");
    }

    @Test
    void portalLinkIsEmailedToTheClientWhenRequested() throws Exception {
        String body = postJson(auth, "/api/app/projects/" + projectId + "/portal-access", """
                {"sendToClient":true}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailSent", is(true)))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");

        Map<String, Object> message = awaitMessageTo(clientEmail, "Seu acesso ao portal");
        assertThat(messageText(message)).contains("/portal/" + token).contains("Padaria Central");
    }

    @Test
    void clientRequestNotifiesTheOwner() throws Exception {
        String token = createPortalLink(auth, projectId);

        portalPost(token, "/requests", """
                {"title":"Trocar a foto da home"}
                """).andExpect(status().isCreated());

        Map<String, Object> message = awaitMessageTo(ownerEmail, "abriu uma nova solicitação");
        assertThat((String) message.get("Subject")).contains("#001").contains("Trocar a foto da home");
    }

    @Test
    void deliveryNotifiesTheClientAndApprovalNotifiesTheOwner() throws Exception {
        String token = createPortalLink(auth, projectId);
        String requestId = createRequest(auth, projectId, "Cardápio de Natal");

        changeStatus(requestId, "IN_PROGRESS");
        changeStatus(requestId, "AWAITING_APPROVAL");
        awaitMessageTo(clientEmail, "aguarda a sua aprovação");

        portalPost(token, "/requests/" + requestId + "/approve", "").andExpect(status().isOk());
        awaitMessageTo(ownerEmail, "aprovou a entrega");
    }

    @Test
    void commentsNotifyTheOtherSide() throws Exception {
        String token = createPortalLink(auth, projectId);
        String requestId = createRequest(auth, projectId, "Ajuste no rodapé");

        portalPost(token, "/requests/" + requestId + "/comments", """
                {"body":"Pode trocar o telefone?"}
                """);
        awaitMessageTo(ownerEmail, "comentou na solicitação");

        postJson(auth, "/api/app/requests/" + requestId + "/comments", """
                {"body":"Feito"}
                """);
        awaitMessageTo(clientEmail, "comentou na solicitação");
    }

    @Test
    void clientWithoutEmailReceivesNothingAndLinkIsNotSent() throws Exception {
        String clientBody = postJson(auth, "/api/app/clients", """
                {"name":"Cliente sem e-mail"}
                """).andReturn().getResponse().getContentAsString();
        String otherProject = createProject(auth, JsonPath.read(clientBody, "$.id"), "Projeto sem e-mail");

        postJson(auth, "/api/app/projects/" + otherProject + "/portal-access", """
                {"sendToClient":true}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailSent", is(false)));
    }

    private void changeStatus(String requestId, String status) throws Exception {
        mvc.perform(patch("/api/app/requests/" + requestId + "/status")
                        .header("Authorization", auth)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"%s"}
                                """.formatted(status)))
                .andExpect(status().isOk());
    }

    private Map<String, Object> awaitMessageTo(String recipient, String subjectPart) {
        return await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(300))
                .until(() -> findMessage(recipient, subjectPart), found -> found != null);
    }

    private Map<String, Object> findMessage(String recipient, String subjectPart) throws Exception {
        String query = URLEncoder.encode("to:\"" + recipient + "\"", StandardCharsets.UTF_8);
        String body = HTTP.send(HttpRequest.newBuilder(URI.create(mailpitApi + "/search?query=" + query)).GET().build(),
                HttpResponse.BodyHandlers.ofString()).body();
        List<Map<String, Object>> messages = JsonPath.read(body, "$.messages");
        return messages.stream()
                .filter(m -> ((String) m.get("Subject")).contains(subjectPart))
                .findFirst()
                .orElse(null);
    }

    private String messageText(Map<String, Object> message) throws Exception {
        String body = HTTP.send(HttpRequest.newBuilder(URI.create(mailpitApi + "/message/" + message.get("ID"))).GET().build(),
                HttpResponse.BodyHandlers.ofString()).body();
        return JsonPath.read(body, "$.Text");
    }
}
