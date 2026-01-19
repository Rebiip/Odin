package testresources;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

import java.util.Map;

public class DownstreamVertxStubTestResource implements QuarkusTestResourceLifecycleManager {

    private Vertx vertx;
    private HttpServer server;

    @Override
    public Map<String, String> start() {
        vertx = Vertx.vertx();

        server = vertx.createHttpServer();

        server.requestHandler(req -> req.bodyHandler(body -> {
            System.out.println("[DEBUG_LOG] Downstream stub received " + req.method() + " " + req.path() + (req.query() == null ? "" : ("?" + req.query())));

            if ("/api/v1/cors-header".equals(req.path())) {
                String origin = req.getHeader("Origin");
                req.response()
                        .putHeader("Content-Type", "text/plain")
                        .putHeader("Access-Control-Allow-Origin", origin == null ? "" : origin)
                        .setStatusCode(200)
                        .end("ok");
                return;
            }

            if ("/api/v1/error-400".equals(req.path())) {
                req.response()
                        .putHeader("Content-Type", "application/json")
                        .putHeader("X-Downstream-Error", "1")
                        .setStatusCode(400)
                        .end("{\"error\":\"bad_request\"}");
                return;
            }

            if ("/api/v1/error-500".equals(req.path())) {
                req.response()
                        .putHeader("Content-Type", "application/json")
                        .putHeader("X-Downstream-Error", "1")
                        .setStatusCode(500)
                        .end("{\"error\":\"internal\"}");
                return;
            }

            String receivedHeader = req.getHeader("X-Test");
            String payload = body == null ? "" : body.toString();
            String responseBody = "method=" + req.method() + " path=" + req.path() + " query=" + (req.query() == null ? "" : req.query()) + " body=" + payload;

            req.response()
                    .putHeader("Content-Type", "text/plain")
                    .putHeader("X-Echo-Method", req.method().name())
                    .putHeader("X-Echo-Path", req.path())
                    .putHeader("X-Received-X-Test", receivedHeader == null ? "" : receivedHeader)
                    .setStatusCode(200)
                    .end(responseBody);
        }));

        server.listen(0).toCompletionStage().toCompletableFuture().join();
        int port = server.actualPort();

        return Map.of(
                "quarkus.oidc.enabled", "false",
                "quarkus.keycloak.policy-enforcer.enabled", "false",
                "gateway.routes.downstream.path-prefixes[0]", "/api/v1",
                "gateway.routes.downstream.strip-prefix", "false",
                "gateway.routes.downstream.target.base-url", "http://localhost:" + port
        );
    }

    @Override
    public void stop() {
        if (server != null) {
            try {
                server.close().toCompletionStage().toCompletableFuture().join();
            } catch (Exception ignored) {
            }
        }
        if (vertx != null) {
            try {
                vertx.close().toCompletionStage().toCompletableFuture().join();
            } catch (Exception ignored) {
            }
        }
    }
}
