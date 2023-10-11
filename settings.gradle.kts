rootProject.name = "wiremock-otel"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include("wiremock-extension")
include("int-test")
