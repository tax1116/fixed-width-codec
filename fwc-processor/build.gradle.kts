plugins {
    id("fwc.kotlin-common")
    id("fwc.publishing")
}

dependencies {
    implementation(project(":fwc-core"))
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}

publishing {
    publications.named<MavenPublication>("maven") {
        artifactId = "fixed-width-codec-processor"
        pom {
            name.set("fixed-width-codec-processor")
            description.set(
                "KSP processor that generates fixed-width record mappers. " +
                    "Consumers put this on the KSP classpath only (ksp(...)), " +
                    "not on the runtime classpath.",
            )
        }
    }
}
