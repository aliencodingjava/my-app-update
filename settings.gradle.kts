@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")

    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")

    }


}





rootProject.name = "supabase-kt"

fun includeSample(name: String, vararg targets: String) {
    targets.forEach {
        include(":sample:$name:$it")
    }
}

rootProject.name = "test"
include(":app")
