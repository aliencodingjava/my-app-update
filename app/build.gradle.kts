import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")                         // use the same Kotlin version as your project
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"   // ✅ REQUIRED
    kotlin("kapt")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20"

}

// ---- read local.properties safely (NO internal APIs) ----
val localProps = Properties().apply {
    val lp = rootProject.file("local.properties")
    if (lp.exists()) lp.inputStream().use { load(it) }
}
val openAiKey: String = localProps.getProperty("OPENAI_API_KEY") ?: ""

val keystoreProperties = Properties().apply {
    val ks = rootProject.file("keystore.properties")
    if (ks.exists()) ks.inputStream().use { load(it) }
}

android {
    namespace = "com.flights.studio"
    compileSdk = 36
    buildToolsVersion = "35.0.0"

    buildFeatures {
        compose = true   // ← enable Compose
        viewBinding = true
        buildConfig = true
    }


    signingConfigs {
        create("release") {
            // guard in case file missing
            if (keystoreProperties.isNotEmpty()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    defaultConfig {
        applicationId = "com.flights.studio"
        minSdk = 31
        targetSdk = 36
        versionCode = 215
        versionName = "0.2.211"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "RELEASE_DATE", "\"Sep-16-2025\"")

        // ✅ from local.properties
        buildConfigField("String", "OPENAI_API_KEY", "\"$openAiKey\"")

        signingConfig = signingConfigs.getByName("release")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "custom-proguard-rules.pro"
            )
            // signingConfig = signingConfigs.getByName("release")
        }

        create("beta") {
            initWith(getByName("release"))
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"

            // QA-friendly:
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true          // allow tools like Stetho, StrictMode logs, etc.

            // proguardFiles(...) would be ignored here, so omit it.
            signingConfig = signingConfigs.getByName("debug")

            // Resolve libs that only define debug/release:
            matchingFallbacks += listOf("release")
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }

    // top-level in module build.gradle.kts
    kotlin {
        // makes the Kotlin compiler emit Java 17 bytecode
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
        // uses a JDK 17 toolchain if available
        jvmToolchain(21)
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }


    lint {
        disable += "NullSafeMutableLiveData"
        checkReleaseBuilds = false
    }
    packaging {
        resources {
            // Either pick the first copy...
            pickFirsts += listOf(
                "META-INF/LICENSE.md",
                "META-INF/NOTICE.md"
            )
            // ...or you could exclude them instead:
            // excludes += listOf("META-INF/LICENSE.md", "META-INF/NOTICE.md")
        }
    }
}

dependencies {
    // ----- Ktor -----
    implementation("io.ktor:ktor-client-core:3.3.0")
    implementation("io.ktor:ktor-client-okhttp:3.3.0")
    implementation("io.ktor:ktor-client-cio:3.3.0")
    implementation("io.ktor:ktor-client-logging:3.3.0")
    implementation("io.ktor:ktor-client-auth:3.3.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.0")

    // ----- JSON / Serialization -----
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")

    // ----- Supabase (BOM) -----
    implementation(platform("io.github.jan-tennert.supabase:bom:3.2.3"))
    implementation("io.github.jan-tennert.supabase:supabase-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt:3.2.3")

    // ----- AndroidX Core -----
    implementation("androidx.activity:activity:1.11.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.appcompat:appcompat-resources:1.7.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.gridlayout:gridlayout:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.10.4")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.3.0")
    implementation("androidx.camera.viewfinder:viewfinder-core:1.5.0")
    implementation("androidx.compose.foundation:foundation-layout:1.9.1")
    implementation("androidx.compose.material3:material3:1.3.2")

    // ----- Jetpack Compose -----
    // Use BOM so all Compose libs match versions automatically
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.09.00"))

    implementation(platform("androidx.compose:compose-bom:2025.09.00"))

    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    // Tooling / Tests (use BOM-managed versions → no version strings here)
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.3")
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.4")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.4")

    // ----- Imaging -----
    implementation("com.github.bumptech.glide:glide:5.0.5")
    kapt("com.github.bumptech.glide:compiler:5.0.5") // ⬅️ not annotationProcessor

    implementation("com.github.piasy:BigImageViewer:1.8.1")
    implementation("com.github.piasy:GlideImageLoader:1.8.1")
    implementation("com.github.MikeOrtiz:TouchImageView:3.6")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")


    // ----- YouTube -----
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.2")
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:chromecast-sender:0.31")

    // ----- Material & Design -----
    implementation("com.google.android.material:material:1.13.0")
    implementation("me.zhanghai.android.materialratingbar:library:1.4.0")
    implementation("com.github.lzyzsd:circleprogress:1.2.1")
    implementation("com.github.QuadFlask:colorpicker:0.0.15")
    implementation("com.github.cachapa:ExpandableLayout:2.9.2")
    implementation("com.wdullaer:materialdatetimepicker:4.2.3")

    // If you’re dropping BlurView everywhere, you can remove these two.
    // Keep them only if other screens still use them.
    implementation("jp.wasabeef:blurry:4.0.1")
    implementation("com.github.Dimezis:BlurView:version-3.0.0")

    // ----- Firebase -----
    implementation(platform("com.google.firebase:firebase-bom:32.2.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.android.gms:play-services-basement:18.8.0")

    // ----- Coroutines -----
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    // ----- Utilities -----
    implementation("com.airbnb.android:lottie:6.6.9")
    implementation("org.apache.commons:commons-text:1.14.0")
    implementation("com.googlecode.libphonenumber:libphonenumber:9.0.14")
    implementation("io.coil-kt:coil:2.7.0")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("com.sun.mail:android-mail:1.6.8")
    implementation("com.sun.mail:android-activation:1.6.8")
    implementation("io.github.oneuiproject:icons:1.1.0")

    // ----- ML Kit -----
    implementation("com.google.mlkit:language-id:17.0.6")

    // ----- Accompanist (optional) -----
    implementation("com.google.accompanist:accompanist-navigation-material:0.36.0")
    implementation("com.google.accompanist:accompanist-pager:0.36.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.36.0")

    // ----- OpenAI -----
    implementation("com.aallam.openai:openai-client:4.0.1")

    // ----- Testing -----
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    // ----- Desugaring -----
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    implementation("com.github.Yanndroid:SamsungOneUi:1.2.2")
    implementation("androidx.dynamicanimation:dynamicanimation:1.1.0")

    // ----- AndroidLiquidGlass (Liquid Glass Bottom Bar) -----
    implementation("com.github.Kyant0:AndroidLiquidGlass:1.0.0-alpha09")
//    implementation("com.github.Kyant0.AndroidLiquidGlass:backdrop:1.0.0-alpha11")

    implementation("androidx.compose.material:material-ripple")
    implementation("io.coil-kt:coil-compose:2.7.0")

}
