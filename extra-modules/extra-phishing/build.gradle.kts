import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kordex-module`
    `published-module`
    `dokka-module`
    `disable-explicit-api-mode`

    kotlin("plugin.serialization")
}

repositories {
    maven {
        name = "KotDis"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }
}

dependencies {
    detektPlugins(libs.detekt)

    implementation(libs.jsoup)

    implementation(libs.logging)
    implementation(libs.kotlin.stdlib)
    implementation(libs.ktor.logging)
    implementation(libs.ktor.cn)
    implementation(libs.ktor.json)
    implementation(libs.ktor.okhttp)

    implementation(project(":kord-extensions"))
}
val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions {
    languageVersion = "1.7"
    freeCompilerArgs = listOf("-Xcontext-receivers")
}
group = "com.kotlindiscord.kord.extensions"
