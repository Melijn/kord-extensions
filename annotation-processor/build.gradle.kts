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

    detektPlugins(libs.detekt)
    detektPlugins(libs.detekt.libraries)
}

dokkaModule {
    moduleName.set("JDA Extensions: Annotation Processor")
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
