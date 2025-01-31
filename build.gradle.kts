import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        maven {
            name = "MelijnRepo"
            url = uri("https://reposilite.melijn.com/releases/")
        }
    }
}

plugins {
    `maven-publish`

    kotlin("jvm")

    id("com.github.jakemarsden.git-hooks")
}

val projectVersion: String by project

group = "me.melijn.kord.extensions"
version = projectVersion

val printVersion = task("printVersion") {
    doLast {
        print(version.toString())
    }
}

gitHooks {
    setHooks(mapOf("pre-commit" to "applyLicenses detekt"))
}

repositories {
    google()
    mavenCentral()

    maven {
        name = "Sonatype Snapshots"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }

    maven {
        name = "Kotlin Discord"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }

    maven {
        url = uri("https://reposilite.melijn.com/shitpack")
    }
}

subprojects {
    group = "me.melijn.kord.extensions"
    version = projectVersion

    tasks.withType<KotlinCompile> {
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.contracts.ExperimentalContracts"
        kotlinOptions.freeCompilerArgs += "-Xskip-prerelease-check"
    }

    repositories {
        rootProject.repositories.forEach {
            if (it is MavenArtifactRepository) {
                maven {
                    name = it.name
                    url = it.url
                }
            }
        }
    }
}
