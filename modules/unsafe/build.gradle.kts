import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kordex-module`
    `published-module`
    `dokka-module`
    `ksp-module`
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(project(":kord-extensions"))

    detektPlugins(libs.detekt)
    detektPlugins(libs.detekt.libraries)

    testImplementation(libs.groovy)  // For logback config
    testImplementation(libs.junit)
    testImplementation(libs.logback)

    ksp(project(":annotation-processor"))
}
val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions {
    languageVersion = "1.7"
    freeCompilerArgs = listOf("-Xcontext-receivers")
}

dokkaModule {
    moduleName.set("Kord Extensions: Unsafe")
}
