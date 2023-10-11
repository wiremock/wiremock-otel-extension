package hacktoberfest.wiremock.otel;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.opentelemetry.sdk.trace.samplers.Sampler.alwaysOn;
import static io.opentelemetry.sdk.trace.samplers.Sampler.parentBased;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.*;
import io.micrometer.tracing.propagation.Propagator;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest
public class TracingServeEventListenerIntTest {
    public HttpClient httpClient;

    private Tracer tracer;
    private Propagator propagator;

    @BeforeEach
    public void setup() {
        httpClient =
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        final var spanExporter = OtlpHttpSpanExporter.builder().build();
        final var sdkTracerProvider = SdkTracerProvider.builder()
                .setResource(Resource.create(Attributes.of(SERVICE_NAME, "int-test")))
                .setSampler(parentBased(alwaysOn()))
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();
        final var propagators = ContextPropagators.create(TextMapPropagator.composite(
                W3CTraceContextPropagator.getInstance(), W3CBaggagePropagator.getInstance()));
        final var openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(propagators)
                .build();
        final var otelTracer = openTelemetrySdk.getTracerProvider().get("hacktoberfest.wiremock.otel");
        final var otelCurrentTraceContext = new OtelCurrentTraceContext();
        final var slf4JEventListener = new Slf4JEventListener();
        final var slf4JBaggageEventListener = new Slf4JBaggageEventListener(Collections.emptyList());
        tracer = new OtelTracer(
                otelTracer,
                otelCurrentTraceContext,
                event -> {
                    slf4JEventListener.onEvent(event);
                    slf4JBaggageEventListener.onEvent(event);
                },
                new OtelBaggageManager(otelCurrentTraceContext, Collections.emptyList(), Collections.emptyList()));
        propagator = new OtelPropagator(propagators, otelTracer);
    }

    @Test
    public void iCanTraceAWiremockRequest(final WireMockRuntimeInfo runtimeInfo) throws Exception {
        stubFor(get(urlPathEqualTo("/")).willReturn(aResponse().withStatus(200)));

        final Span span = tracer.nextSpan().name("iCanTraceAWiremockRequest");
        final var request =
                HttpRequest.newBuilder(URI.create(runtimeInfo.getHttpBaseUrl())).GET();
        propagator.inject(span.context(), request, HttpRequest.Builder::header);
        final var send = httpClient.send(request.build(), HttpResponse.BodyHandlers.discarding());
        assertThat(send.statusCode()).isEqualTo(200);
        span.end();

        final var allServeEvents = getAllServeEvents();
        assertThat(allServeEvents).isNotNull();

        TimeUnit.SECONDS.sleep(2);
    }
}
