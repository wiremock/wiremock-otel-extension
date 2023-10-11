plugins {
    `java-library`
}

dependencies {
    testImplementation(projects.wiremockExtension)
    testImplementation("io.micrometer:micrometer-tracing-bridge-otel")
    testImplementation("io.micrometer:micrometer-registry-otlp")
    testImplementation("io.opentelemetry:opentelemetry-exporter-otlp")
}
