@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")

    }
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
