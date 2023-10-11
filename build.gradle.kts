import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    java
    `project-report`
    alias(libs.plugins.catalog.update)

    alias(libs.plugins.spotless)
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
    }


    testing {
        suites {
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

    dependencies {
        testImplementation(platform(rootProject.libs.junit))
        testImplementation("org.junit.jupiter:junit-jupiter-api")
        testImplementation("org.junit.jupiter:junit-jupiter-params")
        testImplementation(rootProject.libs.assertj)
        testImplementation(rootProject.libs.mockito.junit)

        testRuntimeOnly(platform(rootProject.libs.log4j))
        testRuntimeOnly("org.apache.logging.log4j:log4j-api")
        testRuntimeOnly("org.apache.logging.log4j:log4j-core")
        testRuntimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl")

        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    }

    spotless {
        java {
            palantirJavaFormat()
            removeUnusedImports()
            toggleOffOn()
            cleanthat()
                .addMutator("SafeButNotConsensual")
                .addMutator("SafeButControversial")
                .addMutator("LocalVariableTypeInference")
                // Allow ternary operators
                .excludeMutator("AvoidInlineConditionals")
                // Allow comparison with literal last
                .excludeMutator("LiteralsFirstInComparisons")
                // This gets very confused with e.g. ISO8601 dates
                .excludeMutator("UseUnderscoresInNumericLiterals")
                .sourceCompatibility(java.sourceCompatibility.toString())
        }
    }

}
