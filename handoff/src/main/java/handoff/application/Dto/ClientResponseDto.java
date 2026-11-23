package handoff.application.Dto;

import handoff.application.Entity.Client;

import java.time.Instant;
import java.util.UUID;

public record ClientResponseDto(
        UUID id,
        String name,
        String contactName,
        String email,
        Instant createdAt
) {

    public static ClientResponseDto from(Client client) {
        return new ClientResponseDto(client.getId(), client.getName(), client.getContactName(),
                client.getEmail(), client.getCreatedAt());
    }
}
