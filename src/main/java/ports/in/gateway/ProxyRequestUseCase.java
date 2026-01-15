package ports.in.gateway;

import application.dtos.gateway.GatewayRequest;
import application.dtos.gateway.GatewayResponse;

public interface ProxyRequestUseCase {
    GatewayResponse proxy(GatewayRequest request);
}
