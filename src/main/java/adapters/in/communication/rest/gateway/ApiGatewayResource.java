package adapters.in.communication.rest.gateway;

import application.dtos.gateway.GatewayRequest;
import application.dtos.gateway.GatewayResponse;
import application.dtos.gateway.GatewayStreamResponse;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;
import ports.in.gateway.ProxyRequestUseCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Path("")
@Blocking
@Slf4j
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.WILDCARD)
public class ApiGatewayResource {

    private final ProxyRequestUseCase proxyRequestUseCase;

    public ApiGatewayResource(ProxyRequestUseCase proxyRequestUseCase) {
        this.proxyRequestUseCase = proxyRequestUseCase;
    }

    @GET
    @Path("{proxyPath:.*}")
    public Response get(@PathParam("proxyPath") String proxyPath, @Context UriInfo uriInfo, @Context HttpHeaders headers) {
        return proxy("GET", proxyPath, uriInfo, headers, null);
    }

    @HEAD
    @Path("{proxyPath:.*}")
    public Response head(@PathParam("proxyPath") String proxyPath, @Context UriInfo uriInfo, @Context HttpHeaders headers) {
        return proxy("HEAD", proxyPath, uriInfo, headers, null);
    }

    @DELETE
    @Path("{proxyPath:.*}")
    public Response delete(@PathParam("proxyPath") String proxyPath, @Context UriInfo uriInfo, @Context HttpHeaders headers) {
        return proxy("DELETE", proxyPath, uriInfo, headers, null);
    }

    @OPTIONS
    @Path("{proxyPath:.*}")
    public Response options(@PathParam("proxyPath") String proxyPath, @Context UriInfo uriInfo, @Context HttpHeaders headers) {
        return proxy("OPTIONS", proxyPath, uriInfo, headers, null);
    }

    @POST
    @Path("{proxyPath:.*}")
    public Response post(@PathParam("proxyPath") String proxyPath, @Context UriInfo uriInfo, @Context HttpHeaders headers, byte[] body) {
        return proxy("POST", proxyPath, uriInfo, headers, body);
    }

    @PUT
    @Path("{proxyPath:.*}")
    public Response put(@PathParam("proxyPath") String proxyPath, @Context UriInfo uriInfo, @Context HttpHeaders headers, byte[] body) {
        return proxy("PUT", proxyPath, uriInfo, headers, body);
    }

    @PATCH
    @Path("{proxyPath:.*}")
    public Response patch(@PathParam("proxyPath") String proxyPath, @Context UriInfo uriInfo, @Context HttpHeaders headers, byte[] body) {
        return proxy("PATCH", proxyPath, uriInfo, headers, body);
    }

    private Response proxy(String method, String proxyPath, UriInfo uriInfo, HttpHeaders headers, byte[] body) {
        String normalizedProxyPath = proxyPath == null ? "" : proxyPath;
        if (normalizedProxyPath.isBlank()) {
            normalizedProxyPath = "/";
        } else if (!normalizedProxyPath.startsWith("/")) {
            normalizedProxyPath = "/" + normalizedProxyPath;
        }
        String rawQuery = uriInfo.getRequestUri().getRawQuery();

        log.info("Gateway received {} {}{}", method, normalizedProxyPath, rawQuery == null ? "" : ("?" + rawQuery));

        Map<String, List<String>> headerMap = new HashMap<>();
        headers.getRequestHeaders().forEach((k, v) -> headerMap.put(k, new ArrayList<>(v)));

        GatewayRequest request = new GatewayRequest(method, normalizedProxyPath, rawQuery, headerMap, body);

        if (isStreamingRequest(method, headers, normalizedProxyPath)) {
            GatewayStreamResponse gatewayResponse = proxyRequestUseCase.proxyStream(request);
            log.info("Gateway responding (stream) with status {} for {} {}", gatewayResponse.status(), method, normalizedProxyPath);
            log.debug("Gateway response (stream) headers={}", gatewayResponse.headers());
            return toJaxRsStreamingResponse(gatewayResponse);
        }

        GatewayResponse gatewayResponse = proxyRequestUseCase.proxy(request);
        log.info("Gateway responding with status {} for {} {}", gatewayResponse.status(), method, normalizedProxyPath);
        log.info("Gateway response headers={} bodyBytes={}",
                gatewayResponse.headers(),
                gatewayResponse.body() == null ? 0 : gatewayResponse.body().length);
        return toJaxRsResponse(gatewayResponse);
    }

    private static boolean isStreamingRequest(String method, HttpHeaders headers, String normalizedProxyPath) {
        if (method == null || !method.equalsIgnoreCase("GET")) {
            return false;
        }

        if (normalizedProxyPath != null && normalizedProxyPath.endsWith("/stream")) {
            return true;
        }

        if (headers == null) {
            return false;
        }

        List<String> accepts = headers.getRequestHeader("Accept");
        if (accepts == null || accepts.isEmpty()) {
            return false;
        }

        for (String accept : accepts) {
            if (accept == null) {
                continue;
            }
            String a = accept.toLowerCase(Locale.ROOT);
            if (a.contains("multipart/x-mixed-replace")) {
                return true;
            }
        }

        return false;
    }

    private static Response toJaxRsResponse(GatewayResponse gatewayResponse) {
        Response.ResponseBuilder builder = Response.status(gatewayResponse.status());

        String contentType = null;
        if (gatewayResponse.headers() != null) {
            for (Map.Entry<String, List<String>> entry : gatewayResponse.headers().entrySet()) {
                String k = entry.getKey();
                List<String> values = entry.getValue();
                if (k == null || values == null) {
                    continue;
                }

                if (k.regionMatches(true, 0, "Access-Control-", 0, "Access-Control-".length())) {
                    continue;
                }

                if (k.equalsIgnoreCase("Content-Type") && !values.isEmpty()) {
                    contentType = values.getFirst();
                    continue;
                }

                for (String value : values) {
                    if (value != null) {
                        builder.header(k, value);
                    }
                }
            }
        }

        if (contentType != null && !contentType.isBlank()) {
            builder.type(contentType);
        }

        if (gatewayResponse.body() != null) {
            byte[] body = gatewayResponse.body();
            builder.entity((StreamingOutput) output -> output.write(body));
        }
        return builder.build();
    }

    private static Response toJaxRsStreamingResponse(GatewayStreamResponse gatewayResponse) {
        Response.ResponseBuilder builder = Response.status(gatewayResponse.status());

        String contentType = null;
        if (gatewayResponse.headers() != null) {
            for (Map.Entry<String, List<String>> entry : gatewayResponse.headers().entrySet()) {
                String k = entry.getKey();
                List<String> values = entry.getValue();
                if (k == null || values == null) {
                    continue;
                }

                if (k.regionMatches(true, 0, "Access-Control-", 0, "Access-Control-".length())) {
                    continue;
                }

                if (k.equalsIgnoreCase("Content-Type") && !values.isEmpty()) {
                    contentType = values.getFirst();
                    continue;
                }

                for (String value : values) {
                    if (value != null) {
                        builder.header(k, value);
                    }
                }
            }
        }

        if (contentType != null && !contentType.isBlank()) {
            builder.type(contentType);
        }

        // Helpful when running behind certain reverse proxies.
        builder.header("Cache-Control", "no-cache");
        builder.header("Pragma", "no-cache");
        builder.header("X-Accel-Buffering", "no");

        InputStream body = gatewayResponse.body();
        if (body != null) {
            builder.entity((StreamingOutput) output -> {
                byte[] buffer = new byte[16 * 1024];
                try (InputStream in = body) {
                    int read;
                    while ((read = in.read(buffer)) >= 0) {
                        if (read == 0) {
                            continue;
                        }
                        output.write(buffer, 0, read);
                        output.flush();
                    }
                } catch (IOException ignored) {
                    // client disconnected / downstream closed; treat as normal for streaming endpoints
                }
            });
        }

        return builder.build();
    }
}
