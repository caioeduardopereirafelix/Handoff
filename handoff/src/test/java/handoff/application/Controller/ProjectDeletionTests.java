package handoff.application.Controller;

import com.jayway.jsonpath.JsonPath;
import handoff.application.ApiTestSupport;
import handoff.application.Config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectDeletionTests extends ApiTestSupport {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Autowired
    private S3Client s3Client;

    @Autowired
    private StorageProperties storageProperties;

    @Test
    void deletingAProjectRemovesEverythingInsideItAndItsFiles() throws Exception {
        String auth = registerOrganization("Org Apagar");
        String clientId = createClient(auth, "Padaria");
        String projectId = createProject(auth, clientId, "Site");
        String otherProjectId = createProject(auth, clientId, "Aplicativo");
        String token = createPortalLink(auth, projectId);
        String requestId = createRequest(auth, projectId, "Trocar foto");
        String otherRequestId = createRequest(auth, otherProjectId, "Tela de login");
        postJson(auth, "/api/app/requests/" + requestId + "/comments", """
                {"body":"Segue o layout"}
                """).andExpect(status().isCreated());
        portalPost(token, "/requests/" + requestId + "/comments", """
                {"body":"Obrigado"}
                """).andExpect(status().isCreated());
        String storageKey = uploadPdf(auth, requestId);

        deleteJson(auth, "/api/app/projects/" + projectId).andExpect(status().isNoContent());

        getJson(auth, "/api/app/projects/" + projectId).andExpect(status().isNotFound());
        getJson(auth, "/api/app/requests/" + requestId).andExpect(status().isNotFound());
        portalGet(token, "/project").andExpect(status().isNotFound());
        getJson(auth, "/api/app/requests/" + otherRequestId).andExpect(status().isOk());
        getJson(auth, "/api/app/projects").andExpect(jsonPath("$.items", hasSize(1)));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(objectExists(storageKey)).isFalse());
    }

    @Test
    void anotherOrganizationCannotDeleteTheProject() throws Exception {
        String auth = registerOrganization("Org Dona");
        String projectId = createProject(auth, createClient(auth, "Cliente"), "Projeto");
        String intruder = registerOrganization("Org Intrusa");

        deleteJson(intruder, "/api/app/projects/" + projectId).andExpect(status().isNotFound());
        getJson(auth, "/api/app/projects/" + projectId).andExpect(status().isOk());
    }

    @Test
    void deletingAProjectThatDoesNotExistReturns404() throws Exception {
        String auth = registerOrganization("Org Vazia");

        deleteJson(auth, "/api/app/projects/" + UUID.randomUUID()).andExpect(status().isNotFound());
    }

    private String uploadPdf(String auth, String requestId) throws Exception {
        byte[] content = "%PDF-1.7 layout".getBytes(StandardCharsets.UTF_8);
        String base = "/api/app/requests/" + requestId + "/attachments";
        String uploadBody = postJson(auth, base + "/upload-url", """
                {"fileName":"layout.pdf","contentType":"application/pdf","sizeBytes":%d}
                """.formatted(content.length))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        HttpRequest put = HttpRequest.newBuilder(URI.create(JsonPath.read(uploadBody, "$.uploadUrl")))
                .header("Content-Type", "application/pdf")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
                .build();
        assertThat(HTTP.send(put, HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(200);
        String attachmentId = JsonPath.read(uploadBody, "$.attachmentId");
        postJson(auth, base + "/" + attachmentId + "/complete", "").andExpect(status().isOk());

        String key = keyOf(JsonPath.read(uploadBody, "$.uploadUrl"));
        assertThat(objectExists(key)).isTrue();
        return key;
    }

    private String keyOf(String uploadUrl) {
        String path = URI.create(uploadUrl).getPath();
        return path.substring(("/" + storageProperties.bucket() + "/").length());
    }

    private boolean objectExists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(storageProperties.bucket()).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }
}
