plugins {
    `kordex-module`
    `published-module`
    `dokka-module`
}

dependencies {
    implementation(libs.kotlin.stdlib)
    detektPlugins(libs.detekt)
    detektPlugins(libs.detekt.libraries)
}

dokkaModule {
    moduleName.set("JDA Extensions: Annotation Processor")
}
