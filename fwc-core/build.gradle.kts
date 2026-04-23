plugins {
    id("fwc.kotlin-common")
    id("fwc.publishing")
}

publishing {
    publications.named<MavenPublication>("maven") {
        artifactId = "fixed-width-codec-core"
        pom {
            name.set("fixed-width-codec-core")
            description.set(
                "Annotations and runtime base class for fixed-width-codec. " +
                    "Consumers put this on the runtime classpath (implementation).",
            )
        }
    }
}
