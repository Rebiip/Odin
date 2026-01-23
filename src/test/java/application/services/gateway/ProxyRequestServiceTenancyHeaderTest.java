package application.services.gateway;

import application.dtos.gateway.DownstreamRequest;
import application.dtos.gateway.DownstreamResponse;
import application.dtos.gateway.DownstreamStreamResponse;
import application.dtos.gateway.GatewayRequest;
import configurations.tenancy.TenantIdProvider;
import domain.gateway.RouteDefinition;
import domain.gateway.TargetDefinition;
import org.junit.jupiter.api.Test;
import ports.out.gateway.DownstreamHttpPort;
import ports.out.gateway.RouteDefinitionsPort;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProxyRequestServiceTenancyHeaderTest {

    @Test
    void shouldForwardTenantIdOnDownstreamHeader() {
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        RouteDefinitionsPort routeDefinitionsPort = () -> List.of(
                new RouteDefinition(
                        "downstream",
                        List.of("/api/v1"),
                        false,
                        new TargetDefinition("http://downstream")
                )
        );

        AtomicReference<DownstreamRequest> captured = new AtomicReference<>();
        DownstreamHttpPort downstreamHttpPort = new DownstreamHttpPort() {
            @Override
            public DownstreamResponse execute(DownstreamRequest request) {
                captured.set(request);
                return new DownstreamResponse(200, Map.of(), null);
            }

            @Override
            public DownstreamStreamResponse executeStream(DownstreamRequest request) {
                captured.set(request);
                return new DownstreamStreamResponse(200, Map.of(), new java.io.ByteArrayInputStream(new byte[0]));
            }
        };

        TenantIdProvider tenantIdProvider = () -> tenantId;

        ProxyRequestService service = new ProxyRequestService(routeDefinitionsPort, downstreamHttpPort, tenantIdProvider);

        GatewayRequest gatewayRequest = new GatewayRequest(
                "GET",
                "/api/v1/test",
                null,
                Map.of("X-Test", List.of("abc")),
                null
        );

        service.proxy(gatewayRequest);

        DownstreamRequest downstreamRequest = captured.get();
        assertNotNull(downstreamRequest);
        assertEquals(List.of(tenantId.toString()), downstreamRequest.headers().get("X-Tenant-Id"));
    }
}
