package configurations.tenancy;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public abstract class TenantScopedEntity {

    private UUID tenantId;
}
