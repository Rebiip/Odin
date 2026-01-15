package configurations.tenancy;

import java.util.UUID;

public interface TenantIdProvider {
    UUID getTenantId();
}
