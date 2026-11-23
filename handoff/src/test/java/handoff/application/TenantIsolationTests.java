package handoff.application;

import com.jayway.jsonpath.JsonPath;
import handoff.application.Entity.AppUser;
import handoff.application.Entity.Client;
import handoff.application.Repository.AppUserRepository;
import handoff.application.Repository.ClientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// O teste que prova o isolamento entre organizações: B usa o próprio token para
// tentar ler, listar, alterar e apagar os recursos de A. Todas as tentativas
// precisam responder 404, como se o recurso não existisse.
class TenantIsolationTests extends ApiTestSupport {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AppUserRepository userRepository;

    private String authA;
    private String authB;
    private String clientOfA;
    private String projectOfA;

    @BeforeEach
    void setUp() throws Exception {
        authA = registerOrganization("Organização A");
        authB = registerOrganization("Organização B");
        clientOfA = createClient(authA, "Cliente de A");
        projectOfA = createProject(authA, clientOfA, "Projeto de A");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void organizationBCannotReadResourcesOfA() throws Exception {
        getJson(authB, "/api/app/clients/" + clientOfA).andExpect(status().isNotFound());
        getJson(authB, "/api/app/projects/" + projectOfA).andExpect(status().isNotFound());
    }

    @Test
    void organizationBListsNothingFromA() throws Exception {
        getJson(authB, "/api/app/clients").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        getJson(authB, "/api/app/projects").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        getJson(authB, "/api/app/projects?clientId=" + clientOfA).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void organizationBCannotChangeResourcesOfA() throws Exception {
        putJson(authB, "/api/app/clients/" + clientOfA, """
                {"name":"Invadido"}
                """).andExpect(status().isNotFound());
        putJson(authB, "/api/app/projects/" + projectOfA, """
                {"name":"Invadido","status":"ARCHIVED"}
                """).andExpect(status().isNotFound());

        // e nada mudou para A
        getJson(authA, "/api/app/clients/" + clientOfA).andExpect(jsonPath("$.name").value("Cliente de A"));
        getJson(authA, "/api/app/projects/" + projectOfA).andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void organizationBCannotDeleteResourcesOfA() throws Exception {
        deleteJson(authB, "/api/app/projects/" + projectOfA).andExpect(status().isNotFound());
        deleteJson(authB, "/api/app/clients/" + clientOfA).andExpect(status().isNotFound());

        getJson(authA, "/api/app/projects/" + projectOfA).andExpect(status().isOk());
        getJson(authA, "/api/app/clients/" + clientOfA).andExpect(status().isOk());
    }

    @Test
    void organizationBCannotAttachProjectToClientOfA() throws Exception {
        postJson(authB, "/api/app/projects", """
                {"clientId":"%s","name":"Projeto pendurado em A"}
                """.formatted(clientOfA))
                .andExpect(status().isNotFound());
    }

    // Segunda camada: mesmo um findAll() sem filtro nenhum só enxerga a
    // organização autenticada, porque o Hibernate aplica o @TenantId.
    @Test
    void hibernateTenantFilterHidesOtherOrganizationsEvenWithoutWhereClause() throws Exception {
        authenticateAs(authB);
        createClientDirectly("Cliente de B");

        List<Client> visibleToB = clientRepository.findAll();

        assertThat(visibleToB).extracting(Client::getName).containsExactly("Cliente de B");
        assertThat(visibleToB).extracting(Client::getId).doesNotContain(UUID.fromString(clientOfA));
    }

    private void createClientDirectly(String name) {
        Client client = new Client();
        client.setName(name);
        clientRepository.saveAndFlush(client);
    }

    private void authenticateAs(String auth) throws Exception {
        String body = getJson(auth, "/api/app/me").andReturn().getResponse().getContentAsString();
        AppUser user = userRepository.findByEmail(JsonPath.read(body, "$.email")).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
