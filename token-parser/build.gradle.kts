plugins {
    `kordex-module`
    `published-module`
    `dokka-module`
    `tested-module`
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.logging) // Basic logging setup

    detektPlugins(libs.detekt)
    detektPlugins(libs.detekt.libraries)

    testApi(libs.jda.lib)
    testImplementation(libs.kx.cor)
    testImplementation(libs.groovy)  // For logback config
    testImplementation(libs.jansi)
    testImplementation(libs.junit)
    testImplementation(libs.koin.test)
    testImplementation(libs.logback)
}

dokkaModule {
    moduleName.set("JDA Extensions: Token Parser")
}
