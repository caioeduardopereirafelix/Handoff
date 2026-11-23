package handoff.application.Config;

import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

// Segunda camada do isolamento. Toda entidade com @TenantId (Client, Project...)
// passa a ter, automaticamente:
//   - "where organization_id = :tenant" em toda consulta, inclusive JPQL;
//   - organization_id preenchido no insert com a organização da requisição.
// Mesmo que um repositório esqueça de filtrar, o Hibernate não devolve dados de
// outra organização.
@Component
@RequiredArgsConstructor
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID>, HibernatePropertiesCustomizer {

    // Sessões sem usuário autenticado (login, cadastro, o próprio filtro JWT
    // carregando o usuário) só tocam tabelas sem @TenantId; o "root" desliga o
    // filtro para elas.
    static final UUID ROOT = new UUID(0, 0);

    private final TenantContext tenantContext;

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        return tenantContext.findOrganizationId().orElse(ROOT);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public boolean isRoot(UUID tenantId) {
        return ROOT.equals(tenantId);
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
