package adapters.out.http;

import application.dtos.gateway.DownstreamRequest;
import application.dtos.gateway.DownstreamResponse;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import ports.out.gateway.DownstreamHttpPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class JdkDownstreamHttpAdapter implements DownstreamHttpPort {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Override
    @Timeout(8000)
    @Retry(maxRetries = 2, delay = 200)
    @CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 5000)
    public DownstreamResponse execute(DownstreamRequest request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(request.url()))
                .timeout(Duration.ofSeconds(8));

        if (request.headers() != null) {
            for (Map.Entry<String, List<String>> header : request.headers().entrySet()) {
                if (header.getKey() == null || header.getValue() == null) {
                    continue;
                }
                for (String value : header.getValue()) {
                    if (value != null) {
                        builder.header(header.getKey(), value);
                    }
                }
            }
        }

        if (request.body() != null && request.body().length > 0) {
            builder.method(request.method(), HttpRequest.BodyPublishers.ofByteArray(request.body()));
        } else {
            builder.method(request.method(), HttpRequest.BodyPublishers.noBody());
        }

        try {
            HttpResponse<byte[]> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            return new DownstreamResponse(
                    response.statusCode(),
                    response.headers().map(),
                    response.body()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
