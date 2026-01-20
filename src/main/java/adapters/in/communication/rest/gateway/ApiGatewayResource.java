package adapters.in.communication.rest.gateway;

import application.dtos.gateway.GatewayRequest;
import application.dtos.gateway.GatewayResponse;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        GatewayResponse gatewayResponse = proxyRequestUseCase.proxy(request);
        log.info("Gateway responding with status {} for {} {}", gatewayResponse.status(), method, normalizedProxyPath);
        log.info("Gateway response headers={} bodyBytes={}",
                gatewayResponse.headers(),
                gatewayResponse.body() == null ? 0 : gatewayResponse.body().length);
        try {
            return toJaxRsResponse(gatewayResponse);
        } catch (RuntimeException e) {
            log.error("Failed to map gateway response to JAX-RS response. status={} headers={}",
                    gatewayResponse.status(),
                    gatewayResponse.headers(),
                    e);
            throw e;
        }
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
}
