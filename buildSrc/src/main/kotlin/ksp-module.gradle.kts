plugins {
    java
    idea
    id("com.google.devtools.ksp")
}

tasks { // Hack to get KSP to pick up the module definitions
    afterEvaluate {
        "kspKotlin" {
            if (project.name != "kord-extensions") {
                dependsOn(":kord-extensions:build")
            }
        }
    }
}

idea { // We use this instead of sourceSets b/c we're all IJ users and this fixes build optimisations
    module {
        // Not using += due to https://github.com/gradle/gradle/issues/8749
        sourceDirs = sourceDirs +
            file("$buildDir/generated/ksp/main/kotlin")

        testSourceDirs = testSourceDirs +
            file("$buildDir/generated/ksp/test/kotlin")

        generatedSourceDirs = generatedSourceDirs +
            file("$buildDir/generated/ksp/main/kotlin") +
            file("$buildDir/generated/ksp/test/kotlin")
    }
}
