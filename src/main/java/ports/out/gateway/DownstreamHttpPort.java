package ports.out.gateway;

import application.dtos.gateway.DownstreamRequest;
import application.dtos.gateway.DownstreamResponse;

public interface DownstreamHttpPort {
    DownstreamResponse execute(DownstreamRequest request);
}
