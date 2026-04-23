plugins {
    // Version catalog aliases are not usable in settings.gradle.kts plugins block
    // under current Gradle, so this one plugin's version lives here.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = "fixed-width-codec"

include(":fwc-core", ":fwc-processor")
