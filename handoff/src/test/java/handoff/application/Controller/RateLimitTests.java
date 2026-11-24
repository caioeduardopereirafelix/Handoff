package handoff.application.Controller;

import handoff.application.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RateLimitTests extends ApiTestSupport {

    @Test
    void loginIsBlockedAfterFiveAttemptsForTheSameEmail() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"organizationName":"Org Limite","name":"Dono","email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated());

        for (int attempt = 0; attempt < 5; attempt++) {
            attemptLogin(email, "senha-errada").andExpect(status().isUnauthorized());
        }

        attemptLogin(email, PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.detail", startsWith("Muitas tentativas")));
    }

    @Test
    void blockingOneEmailDoesNotAffectAnother() throws Exception {
        String blocked = uniqueEmail();
        for (int attempt = 0; attempt < 6; attempt++) {
            attemptLogin(blocked, "qualquer");
        }

        attemptLogin(uniqueEmail(), "qualquer").andExpect(status().isUnauthorized());
    }

    private ResultActions attemptLogin(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password)));
    }
}
