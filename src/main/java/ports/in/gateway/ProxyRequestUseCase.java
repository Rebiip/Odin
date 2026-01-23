package ports.in.gateway;

import application.dtos.gateway.GatewayRequest;
import application.dtos.gateway.GatewayResponse;
import application.dtos.gateway.GatewayStreamResponse;

public interface ProxyRequestUseCase {
    GatewayResponse proxy(GatewayRequest request);

    /**
     * Proxies a request in streaming mode (no full-body buffering).
     * Intended for long-lived responses such as MJPEG (multipart/x-mixed-replace).
     */
    GatewayStreamResponse proxyStream(GatewayRequest request);
}
