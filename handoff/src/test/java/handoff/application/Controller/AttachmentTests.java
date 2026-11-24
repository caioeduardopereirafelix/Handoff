package handoff.application.Controller;

import com.jayway.jsonpath.JsonPath;
import handoff.application.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AttachmentTests extends ApiTestSupport {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private String auth;
    private String projectId;
    private String token;
    private String requestId;

    @BeforeEach
    void setUp() throws Exception {
        auth = registerOrganization("Org Anexos");
        String clientId = createClient(auth, "Padaria Central");
        projectId = createProject(auth, clientId, "Site institucional");
        token = createPortalLink(auth, projectId);
        requestId = createRequest(auth, projectId, "Trocar a foto da home");
    }

    @Test
    void developerUploadsAndClientDownloadsTheSameFile() throws Exception {
        byte[] content = "%PDF-1.7 conteudo do layout".getBytes(StandardCharsets.UTF_8);
        String uploadBody = postJson(auth, base() + "/upload-url", """
                {"fileName":"C:\\\\Users\\\\caio\\\\layout final.pdf","contentType":"application/pdf","sizeBytes":%d}
                """.formatted(content.length))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.method", is("PUT")))
                .andExpect(jsonPath("$.expiresAt", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        String attachmentId = JsonPath.read(uploadBody, "$.attachmentId");

        assertThat(put(uploadBody, content, "application/pdf")).isEqualTo(200);

        postJson(auth, base() + "/" + attachmentId + "/complete", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalName", is("layout final.pdf")))
                .andExpect(jsonPath("$.uploadedByType", is("USER")))
                .andExpect(jsonPath("$.completedAt", notNullValue()));

        portalGet(token, "/requests/" + requestId + "/attachments")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sizeBytes", is(content.length)));

        String downloadBody = portalGet(token, "/requests/" + requestId + "/attachments/" + attachmentId + "/download-url")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        HttpResponse<byte[]> download = HTTP.send(
                HttpRequest.newBuilder(URI.create(JsonPath.read(downloadBody, "$.downloadUrl"))).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());

        assertThat(download.statusCode()).isEqualTo(200);
        assertThat(download.body()).isEqualTo(content);
        assertThat(download.headers().firstValue("Content-Disposition").orElse(""))
                .contains("layout%20final.pdf");
    }

    @Test
    void clientUploadsThroughThePortalAndItIsRecordedInTheHistory() throws Exception {
        byte[] content = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
        String uploadBody = portalPost(token, "/requests/" + requestId + "/attachments/upload-url", """
                {"fileName":"fachada.png","contentType":"image/png","sizeBytes":%d}
                """.formatted(content.length))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String attachmentId = JsonPath.read(uploadBody, "$.attachmentId");
        assertThat(put(uploadBody, content, "image/png")).isEqualTo(200);

        portalPost(token, "/requests/" + requestId + "/attachments/" + attachmentId + "/complete", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadedByType", is("CLIENT")))
                .andExpect(jsonPath("$.uploadedByName", is("Padaria Central")));

        getJson(auth, base()).andExpect(jsonPath("$", hasSize(1)));
        getJson(auth, "/api/app/requests/" + requestId + "/activity")
                .andExpect(jsonPath("$[1].action", is("ATTACHMENT_ADDED")))
                .andExpect(jsonPath("$[1].actorType", is("CLIENT")));
    }

    @Test
    void attachmentCanBelongToAComment() throws Exception {
        String commentBody = postJson(auth, "/api/app/requests/" + requestId + "/comments", """
                {"body":"Segue o layout"}
                """).andReturn().getResponse().getContentAsString();
        String commentId = JsonPath.read(commentBody, "$.id");
        byte[] content = "%PDF-1.7".getBytes(StandardCharsets.UTF_8);

        String uploadBody = postJson(auth, base() + "/upload-url", """
                {"fileName":"layout.pdf","contentType":"application/pdf","sizeBytes":%d,"commentId":"%s"}
                """.formatted(content.length, commentId))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        put(uploadBody, content, "application/pdf");

        postJson(auth, base() + "/" + JsonPath.read(uploadBody, "$.attachmentId") + "/complete", "")
                .andExpect(jsonPath("$.commentId", is(commentId)));
    }

    @Test
    void forbiddenTypeAndOversizedFileAreRejected() throws Exception {
        postJson(auth, base() + "/upload-url", """
                {"fileName":"virus.exe","contentType":"application/x-msdownload","sizeBytes":100}
                """).andExpect(status().isBadRequest());
        postJson(auth, base() + "/upload-url", """
                {"fileName":"enorme.pdf","contentType":"application/pdf","sizeBytes":%d}
                """.formatted(10 * 1024 * 1024 + 1)).andExpect(status().isBadRequest());
        postJson(auth, base() + "/upload-url", """
                {"fileName":"vazio.pdf","contentType":"application/pdf","sizeBytes":0}
                """).andExpect(status().isBadRequest());
    }

    @Test
    void completingWithoutUploadingReturns409AndStaysHidden() throws Exception {
        String uploadBody = postJson(auth, base() + "/upload-url", """
                {"fileName":"layout.pdf","contentType":"application/pdf","sizeBytes":10}
                """).andReturn().getResponse().getContentAsString();
        String attachmentId = JsonPath.read(uploadBody, "$.attachmentId");

        postJson(auth, base() + "/" + attachmentId + "/complete", "").andExpect(status().isConflict());
        getJson(auth, base()).andExpect(jsonPath("$", hasSize(0)));
        getJson(auth, base() + "/" + attachmentId + "/download-url").andExpect(status().isNotFound());
    }

    @Test
    void storageRejectsAFileDifferentFromWhatWasAuthorized() throws Exception {
        String uploadBody = postJson(auth, base() + "/upload-url", """
                {"fileName":"layout.pdf","contentType":"application/pdf","sizeBytes":5}
                """).andReturn().getResponse().getContentAsString();

        assertThat(put(uploadBody, "arquivo bem maior que cinco bytes".getBytes(StandardCharsets.UTF_8), "application/pdf"))
                .isEqualTo(403);
        assertThat(put(uploadBody, "12345".getBytes(StandardCharsets.UTF_8), "image/png"))
                .isEqualTo(403);
    }

    @Test
    void fileWhoseContentDoesNotMatchTheDeclaredTypeIsRejectedAndDeleted() throws Exception {
        byte[] content = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);
        String uploadBody = postJson(auth, base() + "/upload-url", """
                {"fileName":"foto.png","contentType":"image/png","sizeBytes":%d}
                """.formatted(content.length))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat(put(uploadBody, content, "image/png")).isEqualTo(200);

        String attachmentId = JsonPath.read(uploadBody, "$.attachmentId");
        postJson(auth, base() + "/" + attachmentId + "/complete", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("O conteúdo do arquivo não corresponde ao tipo informado")));
        postJson(auth, base() + "/" + attachmentId + "/complete", "")
                .andExpect(status().isConflict());
        getJson(auth, base()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void attachmentsAreIsolatedByProjectAndOrganization() throws Exception {
        String otherProject = createProject(auth, createClient(auth, "Academia Fit"), "App de treinos");
        String otherRequest = createRequest(auth, otherProject, "Pedido da academia");
        String otherAuth = registerOrganization("Org Intrusa");

        portalPost(token, "/requests/" + otherRequest + "/attachments/upload-url", """
                {"fileName":"a.pdf","contentType":"application/pdf","sizeBytes":3}
                """).andExpect(status().isNotFound());
        portalGet(token, "/requests/" + otherRequest + "/attachments").andExpect(status().isNotFound());
        getJson(otherAuth, base()).andExpect(status().isNotFound());
        postJson(otherAuth, base() + "/upload-url", """
                {"fileName":"a.pdf","contentType":"application/pdf","sizeBytes":3}
                """).andExpect(status().isNotFound());
    }

    private String base() {
        return "/api/app/requests/" + requestId + "/attachments";
    }

    private int put(String uploadBody, byte[] content, String contentType) throws Exception {
        Map<String, String> headers = JsonPath.read(uploadBody, "$.headers");
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(JsonPath.read(uploadBody, "$.uploadUrl")))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(content));
        headers.forEach((name, value) -> {
            if (!name.equalsIgnoreCase("content-length") && !name.equalsIgnoreCase("content-type")) {
                request.header(name, value);
            }
        });
        request.header("Content-Type", contentType);
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
