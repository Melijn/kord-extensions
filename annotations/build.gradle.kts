plugins {
    `kordex-module`
    `published-module`
    `dokka-module`
}

dependencies {
    implementation(libs.kotlin.stdlib)

}

dokkaModule {
    moduleName.set("Kord Extensions: Annotation Processor")
}
