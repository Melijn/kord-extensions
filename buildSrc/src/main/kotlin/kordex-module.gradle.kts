import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")

    id("org.cadixdev.licenser")
}

val sourceJar = task("sourceJar", Jar::class) {
    dependsOn(tasks["classes"])
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar = task("javadocJar", Jar::class) {
    dependsOn("dokkaJavadoc")
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
    from(tasks.javadoc)
}

tasks {
    build {
        finalizedBy(sourceJar, javadocJar)
    }

    kotlin {
        explicitApi()

        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of("21"))
        }
    }

    jar {
        from(rootProject.file("build/LICENSE-kordex"))
    }

    afterEvaluate {
        rootProject.file("LICENSE").copyTo(rootProject.file("build/LICENSE-kordex"), true)

        tasks.withType<JavaCompile>().configureEach {
            sourceCompatibility = "21"
            targetCompatibility = "21"
        }

        withType<KotlinCompile>().configureEach {
            kotlinOptions {
                jvmTarget = "21"
            }
        }
    }
}


license {
    setHeader(rootProject.file("LICENSE"))
    ignoreFailures(System.getenv()["CI"] == null)
}
