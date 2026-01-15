package domain.gateway;

import java.util.Objects;

public record TargetDefinition(String baseUrl) {

    public TargetDefinition {
        Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        if (baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }
    }
}
