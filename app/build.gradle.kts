
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.util.Properties



plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

val props = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun buildConfigString(value: String): String {
    return "\"" + value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r") + "\""
}

val supabaseUrl: String = props.getProperty("SUPABASE_URL")
    ?: System.getenv("SUPABASE_URL")
    ?: ""
val supabaseAnonKey: String = props.getProperty("SUPABASE_ANON_KEY")
    ?: System.getenv("SUPABASE_ANON_KEY")
    ?: ""



val keystoreProperties = Properties().apply {
    val ks = rootProject.file("keystore.properties")
    if (ks.exists()) ks.inputStream().use { load(it) }
}
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}


android {
    namespace = "com.flights.studio"
    compileSdk = 37

    buildFeatures {
        compose = true   // ← enable Compose
        viewBinding = true
        buildConfig = true
        mlModelBinding = true
    }
    sourceSets {
        val beta = maybeCreate("beta")
        beta.kotlin.directories.add("build/generated/ksp/beta/kotlin")
        beta.java.directories.add("build/generated/ksp/beta/java")
    }

    signingConfigs {
        create("release") {
            // guard in case file missing
            if (keystoreProperties.isNotEmpty()) {
                val storePath = (keystoreProperties["storeFile"] as String).replace("\\:", ":")
                val storePathFile = File(storePath)
                storeFile = if (storePathFile.isAbsolute) storePathFile else rootProject.file(storePath)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    defaultConfig {
        applicationId = "com.flights.studio"
        minSdk = 26
        targetSdk = 37
        ndk {
            //noinspection ChromeOsAbiSupport,ChromeOsAbiSupport
            abiFilters += listOf("arm64-v8a")
        }
        versionCode = 255
        versionName = "0.2.251"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "RELEASE_DATE", "\"Jun-17-2026\"")

        // ✅ from local.properties
        buildConfigField("String", "SUPABASE_URL", buildConfigString(supabaseUrl))
        buildConfigField("String", "SUPABASE_ANON_KEY", buildConfigString(supabaseAnonKey))

        signingConfig = signingConfigs.getByName("release")
    }

    buildTypes {
        getByName("release") {


            isMinifyEnabled = false
            //noinspection NotShrinkingResources
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

        }
        getByName("debug") {


        }

        create("beta") {
            initWith(getByName("release"))
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"

            // QA-friendly:
            isMinifyEnabled = false
            //noinspection NotShrinkingResources
            isShrinkResources = false
            isDebuggable = true

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
        }
    }
    androidResources {
        noCompress += "tflite"
    }

}

dependencies {
    // ----- Ktor -----
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    // ----- JSON / Serialization -----
    implementation(libs.kotlinx.serialization.json.jvm)

    // ----- Supabase -----
    implementation(platform(libs.bom)) // only if libs.bom is your Supabase BOM
    implementation(libs.supabase.storage)

    // ----- AndroidX Core -----
    implementation(libs.activity)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.appcompat.resources)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.preference.ktx)
    implementation(libs.palette.ktx)
    implementation(libs.gridlayout)
    implementation(libs.work.runtime.ktx)
    implementation(libs.coordinatorlayout)
    implementation(libs.viewfinder.core)
    implementation(libs.foundation.layout)
    implementation(libs.core.splashscreen)
    implementation(libs.webkit)

    implementation(libs.compose.foundation)
    implementation(libs.compose.material)
    androidTestImplementation(libs.compose.ui.test.junit4)

    // ----- Jetpack Compose -----
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.material3)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    // ----- Imaging -----
    implementation(libs.glide)
    implementation(libs.play.services.contextmanager)
    implementation(libs.animation.core)
    implementation(libs.zoomable.image.glide)
    ksp(libs.glide.ksp)

    implementation(libs.bigimageviewer)
    implementation(libs.glideimageloader)
    implementation(libs.touchimageview)
    implementation(libs.photoview)
    implementation(libs.material.icons.extended)

    // ----- YouTube -----
    implementation(libs.core)
    implementation(libs.chromecast.sender)

    // ----- Material & Design -----
    implementation(libs.material)
    implementation(libs.library)
    implementation(libs.circleprogress)
    implementation(libs.colorpicker)
    implementation(libs.expandablelayout)
    implementation(libs.materialdatetimepicker)

    implementation(libs.blurry)
    implementation(libs.blurview)

    // ----- Firebase -----
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.play.services.basement)

    // ----- Coroutines -----
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // ----- Utilities -----
    implementation(libs.lottie)
    implementation(libs.commons.text)
    implementation(libs.libphonenumber)
    implementation(libs.coil)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.android.mail)
    implementation(libs.android.activation)
    implementation(libs.icons)

    // ----- ML Kit -----
    implementation(libs.language.id)

    // ----- Accompanist -----
    implementation(libs.accompanist.navigation.material)
    implementation(libs.accompanist.pager)
    implementation(libs.accompanist.pager.indicators)
    implementation(libs.compose.shimmer.skeleton)

    // ----- OpenAI -----
    implementation(libs.openai.client)

    // ----- Testing -----
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.datastore.preferences)

    // ----- Desugaring -----
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.lottie.compose)

    implementation(libs.samsungoneui)
    implementation(libs.dynamicanimation)

    // ----- AndroidLiquidGlass -----
    implementation(libs.backdrop)
    implementation(libs.capsule)

    implementation(libs.reorderable)
    implementation(libs.lazycolumnscrollbar)

    implementation(libs.coil.compose)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.coil3.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.zoomable)
    implementation(libs.constraintlayout.compose)

    implementation(libs.animation)

    debugImplementation(libs.ui.tooling)
    androidTestImplementation(libs.ui.test.manifest)
    runtimeOnly(libs.material.icons.core)
    implementation(libs.supabase.kt)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.coil.svg)
}
