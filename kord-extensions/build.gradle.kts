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
    `kordex-module`
    `published-module`
    `dokka-module`
    `tested-module`
    `ksp-module`
}

dependencies {
    api(libs.h2)
    api(libs.hikari)
    api(libs.icu4j)  // For translations
    api(libs.koin.core)
    api(libs.koin.logger)

    // jda api
    api(libs.jda.lib)
    api(libs.jda.ktx)

    api(libs.logging) // Basic logging setup
    api(libs.kx.ser)
    api(libs.kx.cor)
    api(libs.kx.date)
    api(libs.sentry)  // Needs to be transitive or bots will start breaking
    api(libs.toml)
    api(libs.pf4j)

    // ktor api
    api(libs.ktor)

    api(project(":annotations"))
    api(project(":token-parser"))

    detektPlugins(libs.detekt)
    detektPlugins(libs.detekt.libraries)

    implementation(libs.bundles.commons)
    implementation(libs.kotlin.stdlib)

    testImplementation(libs.groovy)  // For logback config
    testImplementation(libs.jansi)
    testImplementation(libs.junit)
    testImplementation(libs.koin.test)
    testImplementation(libs.logback)

    ksp(project(":annotation-processor"))
    kspTest(project(":annotation-processor"))
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions {
    languageVersion = "1.7"
    freeCompilerArgs = listOf("-Xcontext-receivers")
}

dokkaModule {
    includes.add("packages.md")
}
