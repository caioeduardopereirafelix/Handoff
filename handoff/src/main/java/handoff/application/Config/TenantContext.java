package handoff.application.Config;

import handoff.application.Entity.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class TenantContext {

    private static final ThreadLocal<Boolean> ACROSS_TENANTS = new ThreadLocal<>();

    public <T> T acrossTenants(Supplier<T> action) {
        Boolean previous = ACROSS_TENANTS.get();
        ACROSS_TENANTS.set(Boolean.TRUE);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                ACROSS_TENANTS.remove();
            } else {
                ACROSS_TENANTS.set(previous);
            }
        }
    }

    public boolean isAcrossTenants() {
        return Boolean.TRUE.equals(ACROSS_TENANTS.get());
    }

    public Optional<UUID> findOrganizationId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof AppUser user) {
            return Optional.of(user.getOrganization().getId());
        }
        if (authentication.getPrincipal() instanceof PortalPrincipal portal) {
            return Optional.of(portal.organizationId());
        }
        return Optional.empty();
    }

    public Optional<AppUser> findCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public Optional<PortalPrincipal> findPortalPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof PortalPrincipal portal) {
            return Optional.of(portal);
        }
        return Optional.empty();
    }

    public UUID getOrganizationId() {
        return findOrganizationId()
                .orElseThrow(() -> new IllegalStateException("Nenhuma organização autenticada na requisição"));
    }
}
