package adapters.in.communication.rest.gateway;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import testresources.DownstreamVertxStubTestResource;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(DownstreamVertxStubTestResource.class)
class ApiGatewayResourceTest {

    @Test
    void shouldProxyGetRequests() {
        given()
                .header("X-Test", "123")
                .when()
                .get("/api/v1/hello?x=1")
                .then()
                .statusCode(200)
                .header("X-Echo-Method", equalTo("GET"))
                .header("X-Echo-Path", equalTo("/api/v1/hello"))
                .header("X-Received-X-Test", equalTo("123"))
                .body(containsString("method=GET"))
                .body(containsString("path=/api/v1/hello"))
                .body(containsString("query=x=1"));
    }

    @Test
    void shouldProxyPostRequestsWithBody() {
        given()
                .header("X-Test", "abc")
                .contentType("text/plain")
                .body("payload")
                .when()
                .post("/api/v1/echo")
                .then()
                .statusCode(200)
                .header("X-Echo-Method", equalTo("POST"))
                .header("X-Echo-Path", equalTo("/api/v1/echo"))
                .header("X-Received-X-Test", equalTo("abc"))
                .body(containsString("body=payload"));
    }

    @Test
    void shouldReturn404WhenNoRouteMatches() {
        given()
                .when()
                .get("/api/v2/unknown")
                .then()
                .statusCode(404)
                .body(containsString("GATEWAY.ROUTE_NOT_FOUND"));
    }

    @Test
    void shouldForwardDownstream400Responses() {
        given()
                .when()
                .get("/api/v1/error-400")
                .then()
                .statusCode(400)
                .header("X-Downstream-Error", equalTo("1"))
                .contentType("application/json")
                .body(equalTo("{\"error\":\"bad_request\"}"));
    }

    @Test
    void shouldForwardDownstream500Responses() {
        given()
                .when()
                .get("/api/v1/error-500")
                .then()
                .statusCode(500)
                .header("X-Downstream-Error", equalTo("1"))
                .contentType("application/json")
                .body(equalTo("{\"error\":\"internal\"}"));
    }

    @Test
    void shouldNotDuplicateCorsAllowOriginHeaderWhenDownstreamSendsItToo() {
        Response response = given()
                .header("Origin", "http://localhost:3000")
                .when()
                .get("/api/v1/cors-header");

        response.then().statusCode(200);

        List<String> allowOriginValues = response.getHeaders().getValues("Access-Control-Allow-Origin");
        assertEquals(1, allowOriginValues.size());
        assertEquals("http://localhost:3000", allowOriginValues.get(0));
    }

    @Test
    void shouldProxyHlsPlaylistAsStream() {
        given()
                .when()
                .get("/api/v1/streaming/hls/123/index.m3u8")
                .then()
                .statusCode(200)
                .header("X-Accel-Buffering", equalTo("no"))
                .header("Cache-Control", equalTo("no-cache"))
                .contentType("application/vnd.apple.mpegurl")
                .body(containsString("#EXTM3U"));
    }
}
