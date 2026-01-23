package application.services.gateway;

import application.dtos.gateway.GatewayRequest;
import application.dtos.gateway.GatewayResponse;
import configurations.tenancy.TenantIdProvider;
import domain.gateway.RouteDefinition;
import domain.gateway.TargetDefinition;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import ports.out.gateway.DownstreamHttpPort;
import ports.out.gateway.RouteDefinitionsPort;

import application.dtos.gateway.DownstreamRequest;
import application.dtos.gateway.DownstreamResponse;
import application.dtos.gateway.DownstreamStreamResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProxyRequestServiceTest {

    @Test
    void shouldForwardStatusAndBodyWhenDownstreamThrowsWebApplicationException() {
        RouteDefinitionsPort routes = () -> List.of(
                new RouteDefinition(
                        "downstream",
                        List.of("/api"),
                        false,
                        new TargetDefinition("http://localhost:12345")
                )
        );

        DownstreamHttpPort downstream = new DownstreamHttpPort() {
            @Override
            public DownstreamResponse execute(DownstreamRequest request) {
                Response response = Response.status(400)
                        .type("application/json")
                        .header("X-Downstream-Error", "1")
                        .entity("{\"message\":\"bad request\"}")
                        .build();
                throw new WebApplicationException(response);
            }

            @Override
            public DownstreamStreamResponse executeStream(DownstreamRequest request) {
                throw new UnsupportedOperationException("Not used in this test");
            }
        };

        TenantIdProvider tenantIdProvider = () -> null;

        ProxyRequestService service = new ProxyRequestService(routes, downstream, tenantIdProvider);

        GatewayRequest gatewayRequest = new GatewayRequest(
                "GET",
                "/api/test",
                null,
                Map.of(),
                null
        );

        GatewayResponse response = service.proxy(gatewayRequest);

        assertEquals(400, response.status());
        assertNotNull(response.headers());
        assertTrue(response.headers().containsKey("X-Downstream-Error"));

        byte[] expectedBody = "{\"message\":\"bad request\"}".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expectedBody, response.body());
    }
}
