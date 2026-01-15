package configurations.tenancy;

import configurations.exceptions.exceptions.WSCredentialsException;
import configurations.exceptions.tags.EnTenancyExceptionTag;
import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTenantIdProviderTest {

    @Test
    void shouldReadTenantIdFromTenantIdClaim() {
        UUID tenantId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        JsonWebToken jwt = jwt(Map.of("tenant_id", tenantId.toString()));
        JwtTenantIdProvider provider = new JwtTenantIdProvider(jwt, null);

        assertEquals(tenantId, provider.getTenantId());
    }

    @Test
    void shouldReadTenantIdFromOrganizationClaim() {
        UUID tenantId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        JsonWebToken jwt = jwt(Map.of(
                "organization", Map.of("id", tenantId.toString())
        ));

        JwtTenantIdProvider provider = new JwtTenantIdProvider(jwt, null);
        assertEquals(tenantId, provider.getTenantId());
    }

    @Test
    void shouldReadTenantIdFromSecurityIdentityAttribute() {
        UUID tenantId = UUID.fromString("44444444-4444-4444-4444-444444444444");

        SecurityIdentity identity = identityWithAttributes(Map.of("tenant_id", tenantId.toString()));
        JwtTenantIdProvider provider = new JwtTenantIdProvider(null, identity);

        assertEquals(tenantId, provider.getTenantId());
    }

    @Test
    void shouldThrowMissingWhenNoTenantClaimPresent() {
        JwtTenantIdProvider provider = new JwtTenantIdProvider(jwt(Map.of()), null);

        WSCredentialsException ex = assertThrows(WSCredentialsException.class, provider::getTenantId);
        assertEquals(EnTenancyExceptionTag.TENANT_ID_MISSING, ex.getTagException());
    }

    @Test
    void shouldThrowInvalidWhenTenantClaimIsNotUuid() {
        JwtTenantIdProvider provider = new JwtTenantIdProvider(jwt(Map.of("tenant_id", "not-a-uuid")), null);

        WSCredentialsException ex = assertThrows(WSCredentialsException.class, provider::getTenantId);
        assertEquals(EnTenancyExceptionTag.TENANT_ID_INVALID, ex.getTagException());
    }

    private static JsonWebToken jwt(Map<String, Object> claims) {
        return jwt(claims, "raw-token");
    }

    private static JsonWebToken jwt(Map<String, Object> claims, String rawToken) {
        Set<String> claimNames = claims.keySet();
        return (JsonWebToken) Proxy.newProxyInstance(
                JwtTenantIdProviderTest.class.getClassLoader(),
                new Class[]{JsonWebToken.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getClaimNames" -> claimNames;
                        case "getClaim" -> claims.get(String.valueOf(args[0]));
                        case "getRawToken" -> rawToken;
                        case "getName" -> "test-user";
                        default -> null;
                    };
                }
        );
    }

    private static SecurityIdentity identityWithAttributes(Map<String, Object> attributes) {
        return (SecurityIdentity) Proxy.newProxyInstance(
                JwtTenantIdProviderTest.class.getClassLoader(),
                new Class[]{SecurityIdentity.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getAttribute" -> attributes.get(String.valueOf(args[0]));
                        case "isAnonymous" -> false;
                        default -> null;
                    };
                }
        );
    }
}
