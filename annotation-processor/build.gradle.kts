plugins {
    `kordex-module`
    `published-module`
    `dokka-module`
}

dependencies {
    implementation(libs.kotlin.stdlib)

    implementation(libs.koin.core)
    implementation(libs.kotlinpoet)
    implementation(libs.ksp)

    implementation(project(":annotations"))

}

dokkaModule {
    moduleName.set("Kord Extensions: Annotation Processor")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of("21"))
    }
}
