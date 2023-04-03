import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kordex-module`
    `published-module`
    `dokka-module`
    `ksp-module`
}

dependencies {
    api(libs.time4j.base)
    api(libs.time4j.tzdata)

    implementation(libs.kotlin.stdlib)

    implementation(project(":kord-extensions"))
    implementation(project(":annotations"))

    ksp(project(":annotation-processor"))

    detektPlugins(libs.detekt)

    testImplementation(libs.groovy)  // For logback config
    testImplementation(libs.jansi)
    testImplementation(libs.junit)
    testImplementation(libs.logback)
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions {
    languageVersion = "1.7"
    freeCompilerArgs = listOf("-Xcontext-receivers")
}

dokkaModule {
    moduleName.set("Kord Extensions: Time4J")
}
