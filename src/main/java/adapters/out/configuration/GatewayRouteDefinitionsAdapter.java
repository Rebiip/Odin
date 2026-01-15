package adapters.out.configuration;

import configurations.gateway.GatewayConfig;
import domain.gateway.RouteDefinition;
import domain.gateway.TargetDefinition;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import ports.out.gateway.RouteDefinitionsPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@RequiredArgsConstructor
public class GatewayRouteDefinitionsAdapter implements RouteDefinitionsPort {

    private final GatewayConfig gatewayConfig;

    @Override
    public List<RouteDefinition> listRoutes() {
        List<RouteDefinition> routes = new ArrayList<>();

        for (Map.Entry<String, GatewayConfig.RouteConfig> entry : gatewayConfig.routes().entrySet()) {
            String id = entry.getKey();
            GatewayConfig.RouteConfig routeConfig = entry.getValue();

            String baseUrl = resolveBaseUrl(routeConfig.target());
            routes.add(new RouteDefinition(
                    id,
                    routeConfig.pathPrefixes(),
                    routeConfig.stripPrefix(),
                    new TargetDefinition(baseUrl)
            ));
        }

        return routes;
    }

    private static String resolveBaseUrl(GatewayConfig.TargetConfig target) {
        if (target.baseUrl().isPresent()) {
            return normalizeBaseUrl(target.baseUrl().get());
        }

        if (target.serviceName().isEmpty()) {
            throw new IllegalArgumentException("Gateway route target must define either base-url or service-name");
        }

        String scheme = target.scheme();
        String host = target.serviceName().get();
        String port = target.port().map(p -> ":" + p).orElse("");
        return normalizeBaseUrl(scheme + "://" + host + port);
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
