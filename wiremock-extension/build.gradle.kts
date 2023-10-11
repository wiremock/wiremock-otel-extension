plugins {
    `java-library`
}

dependencies {
    api(platform(libs.mirometer.tracing))
    api(platform(libs.otel))

    api(libs.wiremock)

    api("io.micrometer:micrometer-core")
    api("io.opentelemetry:opentelemetry-api")
    api(libs.otel.annotations)
    api("io.micrometer:micrometer-tracing")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.micrometer:micrometer-registry-otlp")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    implementation(libs.autoservice.annotations)
    annotationProcessor(libs.autoservice)
}
