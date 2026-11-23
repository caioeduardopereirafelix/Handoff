package handoff.application.Controller;

import handoff.application.ApiTestSupport;
import handoff.application.Entity.AppUser;
import handoff.application.Entity.AuthProvider;
import handoff.application.Entity.Role;
import handoff.application.Repository.AppUserRepository;
import handoff.application.Repository.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClientProjectControllerTests extends ApiTestSupport {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void clientCrud() throws Exception {
        String auth = registerOrganization("Org Clientes");

        String clientId = createClient(auth, "Padaria Central");

        getJson(auth, "/api/app/clients/" + clientId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Padaria Central")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        putJson(auth, "/api/app/clients/" + clientId, """
                {"name":"Padaria Central Ltda","contactName":"Ana","email":"ana@padaria.com"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Padaria Central Ltda")))
                .andExpect(jsonPath("$.contactName", is("Ana")));

        getJson(auth, "/api/app/clients").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));

        deleteJson(auth, "/api/app/clients/" + clientId).andExpect(status().isNoContent());
        getJson(auth, "/api/app/clients/" + clientId).andExpect(status().isNotFound());
    }

    @Test
    void clientWithInvalidDataReturns400() throws Exception {
        String auth = registerOrganization("Org Validação");

        postJson(auth, "/api/app/clients", """
                {"name":"","email":"nao-e-email"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name", notNullValue()))
                .andExpect(jsonPath("$.errors.email", notNullValue()));
    }

    @Test
    void projectCrudAndFilterByClient() throws Exception {
        String auth = registerOrganization("Org Projetos");
        String clientA = createClient(auth, "Cliente A");
        String clientB = createClient(auth, "Cliente B");

        String siteId = createProject(auth, clientA, "Site institucional");
        createProject(auth, clientB, "Loja virtual");

        getJson(auth, "/api/app/projects/" + siteId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId", is(clientA)))
                .andExpect(jsonPath("$.clientName", is("Cliente A")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        getJson(auth, "/api/app/projects").andExpect(jsonPath("$", hasSize(2)));
        getJson(auth, "/api/app/projects?clientId=" + clientA)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Site institucional")));

        putJson(auth, "/api/app/projects/" + siteId, """
                {"name":"Site novo","status":"ARCHIVED"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ARCHIVED")));

        deleteJson(auth, "/api/app/projects/" + siteId).andExpect(status().isNoContent());
        getJson(auth, "/api/app/projects/" + siteId).andExpect(status().isNotFound());
    }

    @Test
    void projectWithUnknownClientReturns404() throws Exception {
        String auth = registerOrganization("Org Cliente Inexistente");

        postJson(auth, "/api/app/projects", """
                {"clientId":"%s","name":"Projeto"}
                """.formatted(UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingClientWithProjectsReturns409() throws Exception {
        String auth = registerOrganization("Org Conflito");
        String clientId = createClient(auth, "Cliente com projeto");
        createProject(auth, clientId, "Projeto");

        deleteJson(auth, "/api/app/clients/" + clientId).andExpect(status().isConflict());
    }

    @Test
    void memberCanReadAndEditButOnlyOwnerCanDelete() throws Exception {
        String ownerAuth = registerOrganization("Org Equipe");
        String clientId = createClient(ownerAuth, "Cliente");

        String memberEmail = createMemberInSameOrganizationAs(ownerAuth);
        String memberAuth = login(memberEmail, PASSWORD);

        getJson(memberAuth, "/api/app/clients/" + clientId).andExpect(status().isOk());
        putJson(memberAuth, "/api/app/clients/" + clientId, """
                {"name":"Cliente editado"}
                """).andExpect(status().isOk());
        deleteJson(memberAuth, "/api/app/clients/" + clientId).andExpect(status().isForbidden());

        deleteJson(ownerAuth, "/api/app/clients/" + clientId).andExpect(status().isNoContent());
    }

    @Test
    void endpointsRequireAuthentication() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/app/clients"))
                .andExpect(status().isUnauthorized());
    }

    // Ainda não existe convite de membros (fica para depois do MVP), então o
    // MEMBER é criado direto no banco, na organização do OWNER logado.
    private String createMemberInSameOrganizationAs(String ownerAuth) throws Exception {
        String body = getJson(ownerAuth, "/api/app/me").andReturn().getResponse().getContentAsString();
        UUID orgId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(body, "$.organizationId"));

        AppUser member = new AppUser();
        member.setOrganization(organizationRepository.findById(orgId).orElseThrow());
        member.setName("Membro");
        member.setEmail(uniqueEmail());
        member.setPassword(passwordEncoder.encode(PASSWORD));
        member.setRole(Role.MEMBER);
        member.setAuthProvider(AuthProvider.LOCAL);
        userRepository.save(member);
        return member.getEmail();
    }
}
