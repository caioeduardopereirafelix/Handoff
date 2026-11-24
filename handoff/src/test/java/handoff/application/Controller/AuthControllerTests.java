package handoff.application.Controller;

import com.jayway.jsonpath.JsonPath;
import handoff.application.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.key=${random.uuid}${random.uuid}",
        "spring.datasource.password=test",
        "rate-limit.login-per-ip.requests=10000",
        "rate-limit.register-per-ip.requests=10000",
        "rate-limit.refresh-per-ip.requests=10000"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerTests {

    private static final String PASSWORD = "senha-forte-123";

    @Autowired
    private MockMvc mvc;

    @Test
    void signupCreatesOrganizationAndOwnerAndReturnsTokens() throws Exception {
        String email = uniqueEmail();

        String body = signup("Estúdio Ação Web", "Caio", email)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("Bearer")))
                .andExpect(jsonPath("$.expiration", is(900000)))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        mvc.perform(get("/api/app/me").header("Authorization", bearer(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.role", is("OWNER")))
                .andExpect(jsonPath("$.organizationName", is("Estúdio Ação Web")))
                .andExpect(jsonPath("$.organizationSlug", org.hamcrest.Matchers.startsWith("estudio-acao-web")));
    }

    @Test
    void signupWithExistingEmailReturns409EvenWithDifferentCase() throws Exception {
        String email = uniqueEmail();
        signup("Org A", "Ana", email).andExpect(status().isCreated());

        signup("Org B", "Bia", email.toUpperCase()).andExpect(status().isConflict());
    }

    @Test
    void signupWithInvalidDataReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"organizationName":"","name":"X","email":"nao-e-email","password":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.organizationName", notNullValue()))
                .andExpect(jsonPath("$.errors.email", notNullValue()))
                .andExpect(jsonPath("$.errors.password", notNullValue()));
    }

    @Test
    void loginReturnsTokensAndRejectsWrongPasswordOrUnknownEmail() throws Exception {
        String email = uniqueEmail();
        signup("Org Login", "Caio", email).andExpect(status().isCreated());

        String body = login(email, PASSWORD).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/app/me").header("Authorization", bearer(body)))
                .andExpect(status().isOk());

        login(email, "senha-errada").andExpect(status().isUnauthorized());
        login(uniqueEmail(), PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsMissingOrForgedToken() throws Exception {
        mvc.perform(get("/api/app/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/app/me").header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.e30.forjado"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesTokenAndOldTokenStopsWorking() throws Exception {
        String first = refreshTokenOf(signup("Org Refresh", "Caio", uniqueEmail())
                .andReturn().getResponse().getContentAsString());

        String rotated = refresh(first).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String second = refreshTokenOf(rotated);
        assertThat(second).isNotEqualTo(first);

        refresh(second).andExpect(status().isOk());
    }

    @Test
    void reusingRevokedRefreshTokenRevokesTheWholeFamily() throws Exception {
        String first = refreshTokenOf(signup("Org Reuse", "Caio", uniqueEmail())
                .andReturn().getResponse().getContentAsString());
        String second = refreshTokenOf(refresh(first).andReturn().getResponse().getContentAsString());

        // um atacante reapresenta o token antigo
        refresh(first).andExpect(status().isUnauthorized());

        // e o token legítimo mais novo também deixa de valer
        refresh(second).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshTokenAndIsIdempotent() throws Exception {
        String token = refreshTokenOf(signup("Org Logout", "Caio", uniqueEmail())
                .andReturn().getResponse().getContentAsString());

        logout(token).andExpect(status().isNoContent());
        refresh(token).andExpect(status().isUnauthorized());
        logout(token).andExpect(status().isNoContent());
        logout("token-que-nao-existe").andExpect(status().isNoContent());
    }

    private ResultActions signup(String organizationName, String name, String email) throws Exception {
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"organizationName":"%s","name":"%s","email":"%s","password":"%s"}
                        """.formatted(organizationName, name, email, PASSWORD)));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password)));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)));
    }

    private ResultActions logout(String refreshToken) throws Exception {
        return mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)));
    }

    private static String bearer(String authResponse) {
        return "Bearer " + JsonPath.read(authResponse, "$.token");
    }

    private static String refreshTokenOf(String authResponse) {
        return JsonPath.read(authResponse, "$.refreshToken");
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
