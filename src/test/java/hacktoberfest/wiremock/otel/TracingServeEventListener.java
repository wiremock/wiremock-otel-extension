package hacktoberfest.wiremock.otel;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest
public class TracingServeEventListener {

    public HttpClient httpClient;

    @BeforeEach
    public void setup() {
        httpClient =
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    }

    @Test
    public void iCanTraceAWiremockRequest(final WireMockRuntimeInfo runtimeInfo) throws Exception {
        stubFor(get(urlPathEqualTo("/")).willReturn(aResponse().withStatus(200)));

        final var request = HttpRequest.newBuilder(URI.create(runtimeInfo.getHttpBaseUrl()))
                .GET()
                .build();
        final var send = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        assertThat(send.statusCode()).isEqualTo(200);
    }
}
