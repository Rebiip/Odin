package configurations.gateway;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.ConfigValidationException;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayConfigKubernetesEnvCollisionTest {

    @Test
    void shouldFailValidationWhenPortValueIsTcpUrl() {
        assertThrows(ConfigValidationException.class, () -> new SmallRyeConfigBuilder()
                .withSources(
                        new MapConfigSource("application", Map.of(
                                "gateway.routes.bifrost.target.port", "tcp://10.110.173.176:80",
                                "gateway.routes.bifrost.path-prefixes[0]", "/bifrost"
                        ), 100)
                )
                .withMapping(GatewayConfig.class)
                .build());
    }

    @Test
    void shouldAllowIntegerPortsWhenProvidedAsNumbers() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(
                        new MapConfigSource("application", Map.of(
                                "gateway.routes.bifrost.target.port", "80",
                                "gateway.routes.mimir.target.port", "80",
                                "gateway.routes.bifrost.path-prefixes[0]", "/bifrost",
                                "gateway.routes.mimir.path-prefixes[0]", "/mimir"
                        ), 100)
                )
                .withMapping(GatewayConfig.class)
                .build();

        assertEquals(80, config.getOptionalValue("gateway.routes.bifrost.target.port", Integer.class).orElseThrow());
        assertEquals(80, config.getOptionalValue("gateway.routes.mimir.target.port", Integer.class).orElseThrow());
    }

    @Test
    void applicationProdYamlShouldAvoidKubernetesPortEnvCollisionVariables() throws IOException {
        String prodYaml = readClasspathResource("application-prod.yaml");

        // If a Kubernetes service is named `bifrost`, K8s injects `BIFROST_PORT=tcp://...` which is NOT an integer.
        assertTrue(!prodYaml.contains("${BIFROST_PORT"), "application-prod.yaml must not reference ${BIFROST_PORT...}");
        assertTrue(!prodYaml.contains("${MIMIR_PORT"), "application-prod.yaml must not reference ${MIMIR_PORT...}");

        assertTrue(prodYaml.contains("BIFROST_SERVICE_PORT") || prodYaml.contains("BIFROST_TARGET_PORT"),
                "application-prod.yaml should use BIFROST_SERVICE_PORT and/or BIFROST_TARGET_PORT");
        assertTrue(prodYaml.contains("MIMIR_SERVICE_PORT") || prodYaml.contains("MIMIR_TARGET_PORT"),
                "application-prod.yaml should use MIMIR_SERVICE_PORT and/or MIMIR_TARGET_PORT");

        // Health probes must never be blocked by Keycloak policy enforcement.
        assertTrue(prodYaml.contains("policy-enforcer"), "application-prod.yaml should configure the Keycloak policy enforcer");
        assertTrue(prodYaml.contains("/q/health"), "application-prod.yaml should explicitly reference /q/health paths");
        assertTrue(prodYaml.contains("enforcement-mode: disabled"),
                "application-prod.yaml should disable enforcement for /q/health (liveness/readiness probes)");

        // Ensure stacktraces are visible in production logs; otherwise Quarkus only prints an error-id.
        assertTrue(prodYaml.contains("%e"), "application-prod.yaml log.console.format should include %e to print stacktraces");
    }

    private static String readClasspathResource(String resourceName) throws IOException {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(in, "Missing test classpath resource: " + resourceName);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private record MapConfigSource(String name, Map<String, String> properties, int ordinal) implements ConfigSource {
        @Override
        public Map<String, String> getProperties() {
            return properties;
        }

        @Override
        public Set<String> getPropertyNames() {
            return properties.keySet();
        }

        @Override
        public String getValue(String propertyName) {
            return properties.get(propertyName);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getOrdinal() {
            return ordinal;
        }
    }
}