package application.dtos.gateway;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public record DownstreamStreamResponse(
        int status,
        Map<String, List<String>> headers,
        InputStream body
) {
}
