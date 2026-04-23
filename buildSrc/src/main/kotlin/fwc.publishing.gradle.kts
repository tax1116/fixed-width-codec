plugins {
    `maven-publish`
}

/**
 * Shared POM metadata for all publishable modules.
 *
 * Consumers apply this plugin, then configure the module-specific
 * `artifactId`, `name`, and `description` in their own build file.
 */
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                url.set("https://github.com/tax1116/fixed-width-codec")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("tax1116")
                        name.set("Hyuntaek Oh")
                        email.set("tax941116@gmail.com")
                    }
                }
                scm {
                    url.set("https://github.com/tax1116/fixed-width-codec")
                    connection.set("scm:git:git://github.com/tax1116/fixed-width-codec.git")
                    developerConnection.set("scm:git:ssh://github.com/tax1116/fixed-width-codec.git")
                }
            }
        }
    }
}
