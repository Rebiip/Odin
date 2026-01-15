package configurations.tenancy;

import configurations.exceptions.exceptions.WSCredentialsException;
import configurations.exceptions.tags.EnTenancyExceptionTag;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.json.JsonString;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@RequestScoped
@RequiredArgsConstructor
public class JwtTenantIdProvider implements TenantIdProvider {

    private static final String TENANT_ID_CLAIM = "tenant_id";
    private static final String ORGANIZATION_CLAIM = "organization";
    private static final String ORGANIZATION_ID_FIELD = "id";

    private final JsonWebToken jwt;

    private final SecurityIdentity identity;

    private UUID cached;

    @Override
    public UUID getTenantId() {
        if (cached != null) {
            return cached;
        }

        Object raw = getTenantIdRaw();
        if (raw == null) {
            if (isUnauthenticatedContext()) {
                return null;
            }
            throw new WSCredentialsException(EnTenancyExceptionTag.TENANT_ID_MISSING);
        }

        try {
            cached = raw instanceof UUID u ? u : UUID.fromString(normalizeUuidRawValue(raw));
            return cached;
        } catch (IllegalArgumentException e) {
            throw new WSCredentialsException(EnTenancyExceptionTag.TENANT_ID_INVALID);
        }
    }

    private boolean isUnauthenticatedContext() {
        if (identity != null && identity.isAnonymous()) {
            return true;
        }

        if (jwt != null) {
            try {
                String rawToken = jwt.getRawToken();
                if (rawToken == null || rawToken.isBlank()) {
                    return true;
                }
            } catch (Exception ignored) {
                // ignore
            }
        }

        return jwt == null && identity == null;
    }

    private static String normalizeUuidRawValue(Object raw) {
        if (raw == null) return null;

        String s;
        switch (raw) {
            case JsonString js -> s = js.getString();
            case CharSequence cs -> s = cs.toString();
            default -> s = String.valueOf(raw);
        }

        if (s == null) return null;
        s = s.trim();

        for (int i = 0; i < 4; i++) {
            s = s.trim();

            if (s.length() >= 4 && s.startsWith("\\\"") && s.endsWith("\\\"")) {
                s = s.substring(2, s.length() - 2);
                continue;
            }

            if (s.length() >= 2 && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) {
                s = s.substring(1, s.length() - 1);
                continue;
            }

            if (s.contains("\\\"")) {
                s = s.replace("\\\"", "\"");
                continue;
            }

            break;
        }

        return s;
    }

    private Object getTenantIdRaw() {
        Object raw = readClaimOrIdentityAttribute();
        if (raw != null) return raw;

        raw = readOrganizationIdFromJwt();
        return raw;
    }

    private Object readClaimOrIdentityAttribute() {
        if (jwt != null && jwt.getClaimNames() != null && jwt.getClaimNames().contains(JwtTenantIdProvider.TENANT_ID_CLAIM)) {
            Object raw = jwt.getClaim(JwtTenantIdProvider.TENANT_ID_CLAIM);
            if (raw != null && !String.valueOf(raw).isBlank()) {
                return raw;
            }
        }

        if (identity != null) {
            Object raw = identity.getAttribute(JwtTenantIdProvider.TENANT_ID_CLAIM);
            if (raw != null && !String.valueOf(raw).isBlank()) {
                return raw;
            }
        }

        return null;
    }

    private Object readOrganizationIdFromJwt() {
        if (jwt == null || jwt.getClaimNames() == null || !jwt.getClaimNames().contains(ORGANIZATION_CLAIM)) {
            return null;
        }

        Object orgClaim = jwt.getClaim(ORGANIZATION_CLAIM);
        return extractOrganizationId(orgClaim);
    }

    @SuppressWarnings("unchecked")
    private Object extractOrganizationId(Object orgClaim) {
        switch (orgClaim) {
            case null -> {
                return null;
            }
            case Map<?, ?> root -> {
                Object directId = root.get(ORGANIZATION_ID_FIELD);
                if (directId != null && !String.valueOf(directId).isBlank()) {
                    return directId;
                }

                for (Object v : root.values()) {
                    Object id = extractOrganizationId(v);
                    if (id != null && !String.valueOf(id).isBlank()) {
                        return id;
                    }
                }
                return null;
            }
            case Collection<?> collection -> {
                for (Object v : collection) {
                    Object id = extractOrganizationId(v);
                    if (id != null && !String.valueOf(id).isBlank()) {
                        return id;
                    }
                }
                return null;
            }
            default -> {
                // Nothing to be done
            }
        }

        return null;
    }
}
