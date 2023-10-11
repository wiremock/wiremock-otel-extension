import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    `java-library`
    `project-report`
    alias(libs.plugins.catalog.update)

    alias(libs.plugins.spotless)
}

repositories {
    mavenCentral()
}

java {

}

testing {
    suites {
        val test by getting(JvmTestSuite::class)
        val integrationTest by registering(JvmTestSuite::class) {
            testType.set(TestSuiteType.INTEGRATION_TEST)

            sources {
                compileClasspath += test.sources.output
                runtimeClasspath += test.sources.output
            }

            dependencies {
                implementation(project())
            }

            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(test)
                    }
                }
            }
        }

        withType<JvmTestSuite> {
            useJUnitJupiter()
            targets {
                all {
                    testTask.configure {
                        testLogging {
                            exceptionFormat = FULL
                            showStandardStreams = true
                            events("skipped", "failed")
                        }
                    }
                }
            }
        }
    }
}

val integrationTestImplementation by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}
val integrationTestRuntimeOnly by configurations.getting {
    extendsFrom(configurations.testRuntimeOnly.get())
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

    implementation(libs.autoservice)

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation(libs.mockito.kotlin)

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

spotless {

}
