package configurations.gateway;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@ConfigMapping(prefix = "gateway")
public interface GatewayConfig {

    Map<String, RouteConfig> routes();

    interface RouteConfig {
        List<String> pathPrefixes();

        @WithDefault("false")
        boolean stripPrefix();

        TargetConfig target();
    }

    interface TargetConfig {
        Optional<String> baseUrl();

        Optional<String> serviceName();

        @WithDefault("http")
        String scheme();

        Optional<Integer> port();
    }
}
