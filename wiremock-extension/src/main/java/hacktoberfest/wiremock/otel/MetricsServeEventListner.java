package hacktoberfest.wiremock.otel;

import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ServeEventListener;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.push.PushMeterRegistry;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MetricsServeEventListner implements ServeEventListener {
    private final PushMeterRegistry meterRegistry;

    public MetricsServeEventListner() {
        final var config = new OtlpConfig() {

            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public Map<String, String> resourceAttributes() {
                return Map.of("service.name", "wiremock");
            }

            @Override
            public Duration step() {
                return Duration.ofSeconds(1);
            }
        };
        this.meterRegistry = new OtlpMeterRegistry(config, Clock.SYSTEM);

        new JvmMemoryMetrics().bindTo(meterRegistry);
        new JvmGcMetrics().bindTo(meterRegistry);
        new JvmThreadMetrics().bindTo(meterRegistry);
    }

    @Override
    public String getName() {
        return "metrics";
    }

    @Override
    public void afterComplete(final ServeEvent serveEvent, final Parameters parameters) {
        final var timer = Timer.builder("wiremock.request.totaltime")
                .description("The total request time from start to finish, minus added delay")
                .tag("stub-name", serveEvent.getStubMapping().getName())
                .tag("path", serveEvent.getStubMapping().getRequest().getUrlPath())
                .tag("code", Integer.toString(serveEvent.getResponse().getStatus()))
                .register(meterRegistry);
        timer.record(serveEvent.getTiming().getServeTime(), TimeUnit.MILLISECONDS);
    }
}
