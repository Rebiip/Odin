package adapters.in.communication.rest.gateway;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import testresources.DownstreamVertxStubTestResource;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(DownstreamVertxStubTestResource.class)
class ApiGatewayMjpegStreamingTest {

    @Test
    void shouldStreamMultipartWithoutBuffering() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        URI uri = URI.create("http://localhost:" + RestAssured.port + "/api/v1/mjpeg");
        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "multipart/x-mixed-replace")
                .GET()
                .build();

        HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        assertEquals(200, resp.statusCode());

        String ct = resp.headers().firstValue("Content-Type").orElse("");
        assertTrue(ct.toLowerCase().contains("multipart/x-mixed-replace"));

        CountDownLatch first = new CountDownLatch(1);
        CountDownLatch second = new CountDownLatch(1);

        Thread reader = new Thread(() -> {
            byte[] pattern = "--frame".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            byte[] window = new byte[pattern.length];
            int wPos = 0;
            int matched = 0;
            try (InputStream in = resp.body()) {
                int b;
                while ((b = in.read()) >= 0) {
                    window[wPos] = (byte) b;
                    wPos = (wPos + 1) % window.length;

                    boolean ok = true;
                    for (int i = 0; i < pattern.length; i++) {
                        if (window[(wPos + i) % window.length] != pattern[i]) {
                            ok = false;
                            break;
                        }
                    }
                    if (ok) {
                        matched++;
                        if (matched == 1) {
                            first.countDown();
                        } else if (matched == 2) {
                            second.countDown();
                            return;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }, "mjpeg-test-reader");
        reader.setDaemon(true);
        reader.start();

        // If the gateway buffers, we would not see the first boundary quickly.
        assertTrue(first.await(500, TimeUnit.MILLISECONDS), "Expected first multipart boundary quickly (streaming)");
        assertTrue(second.await(2500, TimeUnit.MILLISECONDS), "Expected second multipart boundary while connection is still open");
    }
}
