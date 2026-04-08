package configurations.gateway;

import configurations.tenancy.TenantIdProvider;
import domain.gateway.RouteDefinition;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ports.out.gateway.RouteDefinitionsPort;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Registers a Vert.x route that intercepts WebSocket upgrade requests and
 * proxies them to the matching downstream service.
 * <p>
 * HTTP (non-upgrade) traffic continues to be handled by the JAX-RS
 * {@link adapters.in.communication.rest.gateway.ApiGatewayResource}.
 */
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class VertxGatewayProxyRoutes {

    private static final String TENANT_ID_HEADER = "X-Tenant-Id";

    private final RouteDefinitionsPort routeDefinitionsPort;
    private final TenantIdProvider tenantIdProvider;
    private final Vertx vertx;

    public void registerRoutes(@Observes Router router) {
        router.route().order(Integer.MIN_VALUE).handler(ctx -> {
            String upgradeHeader = ctx.request().getHeader("Upgrade");
            if (upgradeHeader == null || !upgradeHeader.equalsIgnoreCase("websocket")) {
                ctx.next();
                return;
            }

            String requestPath = ctx.request().path();
            log.info("WebSocket upgrade request received for path={}", requestPath);

            List<RouteDefinition> routes = routeDefinitionsPort.listRoutes();
            Optional<RouteMatch> matchOpt = findBestMatch(routes, requestPath);

            if (matchOpt.isEmpty()) {
                log.warn("No gateway route found for WebSocket path={}", requestPath);
                ctx.response().setStatusCode(404).end("No route found for WebSocket path: " + requestPath);
                return;
            }

            RouteMatch match = matchOpt.get();
            String downstreamPath = match.route.rewritePath(requestPath, match.matchedPrefix);

            URI targetUri = URI.create(match.route.target().baseUrl());
            String targetHost = targetUri.getHost();
            boolean ssl = "https".equalsIgnoreCase(targetUri.getScheme()) || "wss".equalsIgnoreCase(targetUri.getScheme());
            int targetPort = targetUri.getPort() > 0 ? targetUri.getPort() : (ssl ? 443 : 80);

            log.info("Proxying WebSocket upgrade to {}:{}{} (ssl={})", targetHost, targetPort, downstreamPath, ssl);

            HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions()
                    .setDefaultHost(targetHost)
                    .setDefaultPort(targetPort)
                    .setSsl(ssl)
                    .setTrustAll(true));

            WebSocketConnectOptions wsOptions = new WebSocketConnectOptions()
                    .setHost(targetHost)
                    .setPort(targetPort)
                    .setSsl(ssl)
                    .setURI(downstreamPath);

            // Forward relevant headers
            ctx.request().headers().forEach(entry -> {
                String key = entry.getKey();
                String keyLower = key.toLowerCase();
                if (keyLower.equals("host") || keyLower.equals("upgrade") || keyLower.equals("connection")
                        || keyLower.equals("sec-websocket-key") || keyLower.equals("sec-websocket-version")
                        || keyLower.equals("sec-websocket-extensions") || keyLower.equals("sec-websocket-protocol")) {
                    return;
                }
                wsOptions.addHeader(key, entry.getValue());
            });

            // Add tenant header
            try {
                UUID tenantId = tenantIdProvider.getTenantId();
                if (tenantId != null) {
                    wsOptions.addHeader(TENANT_ID_HEADER, tenantId.toString());
                }
            } catch (Exception e) {
                log.debug("Could not resolve tenant id for WebSocket proxy: {}", e.getMessage());
            }

            // Upgrade the server-side connection and connect to downstream
            ctx.request().toWebSocket()
                    .onSuccess(serverWs -> {
                        log.info("Server WebSocket connection established for path={}", requestPath);

                        httpClient.webSocket(wsOptions)
                                .onSuccess(downstreamWs -> {
                                    log.info("Downstream WebSocket connection established to {}:{}{}", targetHost, targetPort, downstreamPath);
                                    bridgeWebSockets(serverWs, downstreamWs, httpClient);
                                })
                                .onFailure(err -> {
                                    log.error("Failed to connect downstream WebSocket to {}:{}{}: {}", targetHost, targetPort, downstreamPath, err.getMessage());
                                    serverWs.close((short) 1011, "Downstream connection failed");
                                    httpClient.close();
                                });
                    })
                    .onFailure(err -> {
                        log.error("Failed to upgrade server WebSocket for path={}: {}", requestPath, err.getMessage());
                        ctx.response().setStatusCode(500).end("WebSocket upgrade failed");
                        httpClient.close();
                    });
        });
    }

    private void bridgeWebSockets(ServerWebSocket client, WebSocket downstream, HttpClient httpClient) {
        // Client -> Downstream
        client.textMessageHandler(downstream::writeTextMessage);
        client.binaryMessageHandler(downstream::writeBinaryMessage);
        client.pongHandler(downstream::writePong);

        // Downstream -> Client
        downstream.textMessageHandler(client::writeTextMessage);
        downstream.binaryMessageHandler(client::writeBinaryMessage);
        downstream.pongHandler(client::writePong);

        // Close propagation
        client.closeHandler(v -> {
            log.info("Client WebSocket closed, closing downstream");
            downstream.close();
            httpClient.close();
        });

        downstream.closeHandler(v -> {
            log.info("Downstream WebSocket closed, closing client");
            client.close();
            httpClient.close();
        });

        // Error handling
        client.exceptionHandler(err -> {
            log.error("Client WebSocket error: {}", err.getMessage());
            downstream.close();
            httpClient.close();
        });

        downstream.exceptionHandler(err -> {
            log.error("Downstream WebSocket error: {}", err.getMessage());
            client.close();
            httpClient.close();
        });
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

    private record RouteMatch(RouteDefinition route, String matchedPrefix) {
    }
}
