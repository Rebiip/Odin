package application.services.gateway;

import application.dtos.gateway.DownstreamRequest;
import application.dtos.gateway.DownstreamResponse;
import application.dtos.gateway.DownstreamStreamResponse;
import application.dtos.gateway.GatewayRequest;
import application.dtos.gateway.GatewayResponse;
import application.dtos.gateway.GatewayStreamResponse;
import application.exceptions.DownstreamRequestFailedException;
import application.exceptions.RouteNotFoundException;
import configurations.tenancy.TenantIdProvider;
import domain.gateway.RouteDefinition;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ports.in.gateway.ProxyRequestUseCase;
import ports.out.gateway.DownstreamHttpPort;
import ports.out.gateway.RouteDefinitionsPort;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class ProxyRequestService implements ProxyRequestUseCase {

    private static final String TENANT_ID_HEADER = "X-Tenant-Id";

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailers",
            "transfer-encoding",
            "upgrade",
            "host",
            "content-length"
    );

    private final RouteDefinitionsPort routeDefinitionsPort;
    private final DownstreamHttpPort downstreamHttpPort;
    private final TenantIdProvider tenantIdProvider;

    @Override
    public GatewayResponse proxy(GatewayRequest request) {
        log.info("ProxyRequestService.proxy start method={} path={} query={}", request.method(), request.path(), request.rawQuery());
        return doProxy(request);
    }

    @Override
    public GatewayStreamResponse proxyStream(GatewayRequest request) {
        log.info("ProxyRequestService.proxyStream start method={} path={} query={}", request.method(), request.path(), request.rawQuery());
        return doProxyStream(request);
    }

    private GatewayResponse doProxy(GatewayRequest request) {
        List<RouteDefinition> routes = routeDefinitionsPort.listRoutes();

        RouteMatch match = findBestMatch(routes, request.path())
                .orElseThrow(() -> new RouteNotFoundException("No route found for path: " + request.path()));

        String downstreamPath = match.route.rewritePath(request.path(), match.matchedPrefix);

        log.info("Matched route id={} matchedPrefix={} stripPrefix={} baseUrl={} downstreamPath={}",
                match.route.id(), match.matchedPrefix, match.route.stripPrefix(), match.route.target().baseUrl(), downstreamPath);

        String url = match.route.target().baseUrl() + downstreamPath;
        if (request.rawQuery() != null && !request.rawQuery().isBlank()) {
            url = url + "?" + request.rawQuery();
        }

        Map<String, List<String>> downstreamHeaders = withTenantHeader(filterHeaders(request.headers()));

        DownstreamRequest downstreamRequest = new DownstreamRequest(
                request.method(),
                url,
                downstreamHeaders,
                request.body()
        );

        log.info("Executing downstream request url={}", url);

        DownstreamResponse downstreamResponse;
        try {
            downstreamResponse = downstreamHttpPort.execute(downstreamRequest);
        } catch (Exception e) {
            GatewayResponse forwarded = tryForwardEmbeddedHttpResponse(e);
            if (forwarded != null) {
                return forwarded;
            }
            throw new DownstreamRequestFailedException("Downstream request failed", e);
        }

        log.info("Downstream response status={}", downstreamResponse.status());

        return new GatewayResponse(
                downstreamResponse.status(),
                filterHeaders(downstreamResponse.headers()),
                downstreamResponse.body()
        );
    }

    private GatewayStreamResponse doProxyStream(GatewayRequest request) {
        List<RouteDefinition> routes = routeDefinitionsPort.listRoutes();

        RouteMatch match = findBestMatch(routes, request.path())
                .orElseThrow(() -> new RouteNotFoundException("No route found for path: " + request.path()));

        String downstreamPath = match.route.rewritePath(request.path(), match.matchedPrefix);

        log.info("Matched route (stream) id={} matchedPrefix={} stripPrefix={} baseUrl={} downstreamPath={}",
                match.route.id(), match.matchedPrefix, match.route.stripPrefix(), match.route.target().baseUrl(), downstreamPath);

        String url = match.route.target().baseUrl() + downstreamPath;
        if (request.rawQuery() != null && !request.rawQuery().isBlank()) {
            url = url + "?" + request.rawQuery();
        }

        Map<String, List<String>> downstreamHeaders = withTenantHeader(filterHeaders(request.headers()));

        DownstreamRequest downstreamRequest = new DownstreamRequest(
                request.method(),
                url,
                downstreamHeaders,
                request.body()
        );

        log.info("Executing downstream stream request url={}", url);

        DownstreamStreamResponse downstreamResponse;
        try {
            downstreamResponse = downstreamHttpPort.executeStream(downstreamRequest);
        } catch (Exception e) {
            // For streaming, we do not attempt to read/forward embedded HTTP response bodies here.
            throw new DownstreamRequestFailedException("Downstream stream request failed", e);
        }

        log.info("Downstream stream response status={}", downstreamResponse.status());

        return new GatewayStreamResponse(
                downstreamResponse.status(),
                filterHeaders(downstreamResponse.headers()),
                downstreamResponse.body()
        );
    }

    private GatewayResponse tryForwardEmbeddedHttpResponse(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof WebApplicationException webApplicationException) {
                Response response = webApplicationException.getResponse();
                if (response == null) {
                    return null;
                }

                Map<String, List<String>> headers = multivaluedToMap(response.getStringHeaders());
                byte[] body = readEntityAsBytes(response);

                return new GatewayResponse(
                        response.getStatus(),
                        filterHeaders(headers),
                        body
                );
            }
            current = current.getCause();
        }
        return null;
    }

    private static Map<String, List<String>> multivaluedToMap(MultivaluedMap<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> map = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            map.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return map;
    }

    private static byte[] readEntityAsBytes(Response response) {
        if (response == null || !response.hasEntity()) {
            return null;
        }

        try {
            byte[] bytes = response.readEntity(byte[].class);
            return (bytes == null || bytes.length == 0) ? null : bytes;
        } catch (Exception ignored) {
        }

        try {
            String text = response.readEntity(String.class);
            if (text == null || text.isBlank()) {
                return null;
            }
            return text.getBytes(StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Optional<RouteMatch> findBestMatch(List<RouteDefinition> routes, String path) {
        RouteMatch best = null;
        for (RouteDefinition route : routes) {
            Optional<String> matched = route.bestMatchingPrefix(path);
            if (matched.isEmpty()) {
                continue;
            }
            if (best == null || matched.get().length() > best.matchedPrefix.length()) {
                best = new RouteMatch(route, matched.get());
            }
        }
        return Optional.ofNullable(best);
    }

    private static Map<String, List<String>> filterHeaders(Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> filtered = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }

            // HTTP/2 pseudo-headers (e.g. ":status") must never be forwarded as HTTP headers.
            if (entry.getKey().startsWith(":")) {
                continue;
            }

            String normalized = entry.getKey().toLowerCase(Locale.ROOT);
            if (HOP_BY_HOP_HEADERS.contains(normalized)) {
                continue;
            }
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            filtered.put(entry.getKey(), entry.getValue());
        }

        return filtered;
    }

    private Map<String, List<String>> withTenantHeader(Map<String, List<String>> headers) {
        UUID tenantId = tenantIdProvider.getTenantId();
        if (tenantId == null) {
            return headers;
        }

        Map<String, List<String>> enriched = new HashMap<>(headers == null ? Map.of() : headers);
        enriched.put(TENANT_ID_HEADER, List.of(tenantId.toString()));
        return enriched;
    }

    private record RouteMatch(RouteDefinition route, String matchedPrefix) {
    }
}
