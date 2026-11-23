package handoff.application.Config;

import handoff.application.Entity.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

// Fonte única da organização da requisição. A regra de ouro: o orgId nunca vem
// do corpo, da URL ou de um header — sai sempre do usuário autenticado pelo
// JwtAuthenticationFilter.
@Component
public class TenantContext {

    public Optional<UUID> findOrganizationId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUser user) {
            return Optional.of(user.getOrganization().getId());
        }
        return Optional.empty();
    }

    public UUID getOrganizationId() {
        return findOrganizationId()
                .orElseThrow(() -> new IllegalStateException("Nenhuma organização autenticada na requisição"));
    }
}
