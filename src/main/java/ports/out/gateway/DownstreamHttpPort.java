package ports.out.gateway;

import application.dtos.gateway.DownstreamRequest;
import application.dtos.gateway.DownstreamResponse;
import application.dtos.gateway.DownstreamStreamResponse;

public interface DownstreamHttpPort {
    DownstreamResponse execute(DownstreamRequest request);

    /**
     * Executes a downstream request and returns a streaming body.
     */
    DownstreamStreamResponse executeStream(DownstreamRequest request);
}
