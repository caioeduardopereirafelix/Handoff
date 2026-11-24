package handoff.application;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Base dos testes de API: mesmo contexto Spring (reaproveitado entre as
// classes), Postgres no Testcontainers e atalhos para chamar a API autenticado.
@SpringBootTest(properties = {
        "jwt.key=${random.uuid}${random.uuid}",
        "spring.datasource.password=test",
        "rate-limit.login-per-ip.requests=10000",
        "rate-limit.register-per-ip.requests=10000",
        "rate-limit.refresh-per-ip.requests=10000"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class ApiTestSupport {

    protected static final String PASSWORD = "senha-forte-123";

    @Autowired
    protected MockMvc mvc;

    // Cria uma organização nova com seu OWNER e devolve o header Authorization.
    protected String registerOrganization(String organizationName) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"organizationName":"%s","name":"Dono","email":"%s","password":"%s"}
                                """.formatted(organizationName, uniqueEmail(), PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    protected String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    protected String createClient(String auth, String name) throws Exception {
        String body = postJson(auth, "/api/app/clients", """
                {"name":"%s","contactName":"Contato","email":"contato@example.com"}
                """.formatted(name))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    protected String createProject(String auth, String clientId, String name) throws Exception {
        String body = postJson(auth, "/api/app/projects", """
                {"clientId":"%s","name":"%s"}
                """.formatted(clientId, name))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    protected String createPortalLink(String auth, String projectId) throws Exception {
        String body = postJson(auth, "/api/app/projects/" + projectId + "/portal-access", "{}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    protected String createRequest(String auth, String projectId, String title) throws Exception {
        String body = postJson(auth, "/api/app/projects/" + projectId + "/requests", """
                {"title":"%s"}
                """.formatted(title))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    protected ResultActions portalGet(String token, String path) throws Exception {
        return mvc.perform(get("/api/portal/" + token + path));
    }

    protected ResultActions portalPost(String token, String path, String json) throws Exception {
        return mvc.perform(post("/api/portal/" + token + path)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions getJson(String auth, String url) throws Exception {
        return mvc.perform(get(url).header("Authorization", auth));
    }

    protected ResultActions postJson(String auth, String url, String json) throws Exception {
        return mvc.perform(post(url).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions putJson(String auth, String url, String json) throws Exception {
        return mvc.perform(put(url).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions deleteJson(String auth, String url) throws Exception {
        return mvc.perform(delete(url).header("Authorization", auth));
    }

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
