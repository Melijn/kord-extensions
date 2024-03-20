plugins {
    `maven-publish`
    signing
}

val sourceJar: Task by tasks.getting
val javadocJar: Task by tasks.getting

publishing {
    repositories {
        maven {
            name = "Melijn"

            url = if (project.version.toString().contains("SNAPSHOT")) {
                uri("https://reposilite.melijn.com/snapshots/")
            } else {
                uri("https://reposilite.melijn.com/releases/")
            }

            credentials {
                username = project.findProperty("melijnReposilitePub") as String? ?: System.getenv("KOTLIN_DISCORD_USER")
                password = project.findProperty("melijnReposilitePassword") as String?
                    ?: System.getenv("KOTLIN_DISCORD_PASSWORD")
            }

            version = project.version
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components.getByName("java"))

            artifact(sourceJar)
            artifact(javadocJar)

            pom {
                name.set("JDA Extensions")
                description.set("A fork of Kord Extensions to work with JDA")
                packaging = "jar"

                scm {
                    connection.set("scm:git:https://github.com/Melijn/kord-extensions.git")
                    developerConnection.set("scm:git:git@github.com:Melijn/kord-extensions.git")
                    url.set("https://github.com/Melijn/kord-extensions.git")
                }

                licenses {
                    license {
                        name.set("Mozilla Public License Version 2.0")
                        url.set("https://www.mozilla.org/en-US/MPL/2.0/")
                    }
                }
            }
        }
    }

    signing {
        useGpgCmd()
        sign(publishing.publications["maven"])
    }
}
