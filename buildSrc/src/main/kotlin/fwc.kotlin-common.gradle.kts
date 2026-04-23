plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

group = "io.github.tax1116"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    "testImplementation"(kotlin("test"))
    "testImplementation"(libs.junit.api)
    "testRuntimeOnly"(libs.junit.engine)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}
