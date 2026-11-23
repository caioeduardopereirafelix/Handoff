package handoff.application.Service;

import handoff.application.Config.TenantContext;
import handoff.application.Dto.ClientRequestDto;
import handoff.application.Dto.ClientResponseDto;
import handoff.application.Entity.Client;
import handoff.application.Exception.ClientHasProjectsException;
import handoff.application.Exception.ResourceNotFoundException;
import handoff.application.Repository.ClientRepository;
import handoff.application.Repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final ProjectRepository projectRepository;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public List<ClientResponseDto> list() {
        return clientRepository.findAllByOrganizationIdOrderByNameAsc(tenantContext.getOrganizationId()).stream()
                .map(ClientResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientResponseDto get(UUID id) {
        return ClientResponseDto.from(find(id));
    }

    // organization_id não é preenchido aqui: o Hibernate coloca o da
    // requisição no insert (@TenantId).
    @Transactional
    public ClientResponseDto create(ClientRequestDto request) {
        Client client = new Client();
        apply(client, request);
        return ClientResponseDto.from(clientRepository.saveAndFlush(client));
    }

    @Transactional
    public ClientResponseDto update(UUID id, ClientRequestDto request) {
        Client client = find(id);
        apply(client, request);
        return ClientResponseDto.from(clientRepository.saveAndFlush(client));
    }

    @Transactional
    public void delete(UUID id) {
        Client client = find(id);
        if (projectRepository.existsByOrganizationIdAndClientId(tenantContext.getOrganizationId(), id)) {
            throw new ClientHasProjectsException();
        }
        clientRepository.delete(client);
    }

    // Usado também pelo ProjectService: cliente de outra organização = 404.
    Client find(UUID id) {
        return clientRepository.findByIdAndOrganizationId(id, tenantContext.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente"));
    }

    private static void apply(Client client, ClientRequestDto request) {
        client.setName(request.name().trim());
        client.setContactName(blankToNull(request.contactName()));
        client.setEmail(blankToNull(request.email()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
