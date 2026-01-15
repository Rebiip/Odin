package application.dtos.gateway;

import java.util.List;
import java.util.Map;

public record DownstreamRequest(
        String method,
        String url,
        Map<String, List<String>> headers,
        byte[] body
) {
}
