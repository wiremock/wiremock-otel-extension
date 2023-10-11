package hacktoberfest.wiremock.otel;

import static io.opentelemetry.sdk.trace.samplers.Sampler.alwaysOn;
import static io.opentelemetry.sdk.trace.samplers.Sampler.parentBased;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;

import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ServeEventListener;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.SubEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
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
import java.util.Collections;
import java.util.Map;

public class TracingServeEventListener implements ServeEventListener {
    private static final String SUB_EVENT_NAME = "tracing-event";
    private final Tracer tracer;
    private final Propagator propagator;

    public TracingServeEventListener() {
        final var spanExporter = OtlpHttpSpanExporter.builder().build();
        final var sdkTracerProvider = SdkTracerProvider.builder()
                .setResource(Resource.create(Attributes.of(SERVICE_NAME, "wiremock-otel")))
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

    @Override
    public String getName() {
        return "tracing";
    }

    @Override
    public void beforeMatch(final ServeEvent serveEvent, final Parameters parameters) {
        final var extractedSpan = propagator
                .extract(serveEvent.getRequest(), LoggedRequest::getHeader)
                .kind(Span.Kind.SERVER)
                .remoteIpAndPort(
                        serveEvent.getRequest().getClientIp(),
                        serveEvent.getRequest().getPort())
                .start();
        final var spanInScope = tracer.withSpan(extractedSpan);
        serveEvent.appendSubEvent(
                new SubEvent(SUB_EVENT_NAME, 0L, Map.of("span", extractedSpan, "scope", spanInScope)));
    }

    @Override
    public void afterComplete(final ServeEvent serveEvent, final Parameters parameters) {
        serveEvent.getSubEvents().stream()
                .filter(e -> e.getType().equals(SUB_EVENT_NAME))
                .map(SubEvent::getData)
                .findFirst()
                .ifPresent(data -> {
                    final var scope = (Tracer.SpanInScope) data.get("scope");
                    scope.close();
                    final var span = (Span) data.get("span");
                    span.end();
                });
    }
}
