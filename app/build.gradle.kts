plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {

    namespace = "com.flights.studio"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/LICENSE.md"
        }
    }

    defaultConfig {
        applicationId = "com.flights.studio"
        minSdk = 30
        targetSdk = 35
        versionCode = 168
        versionName = "0.2.164"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "RELEASE_DATE", "\"Feb-17-2025\"")
        signingConfig = signingConfigs.getByName("debug")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            renderscriptOptimLevel = 3
        }

        create("beta") {
            initWith(buildTypes.getByName("release"))
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "custom-proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    dependenciesInfo {
        includeInApk = true
        includeInBundle = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
}

dependencies {
    // Compose dependencies
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui:1.7.8")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.foundation:foundation:1.7.8")
    implementation("androidx.core:core-animation:1.0.0")
    implementation("androidx.compose.runtime:runtime-saved-instance-state:1.0.0-alpha11")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.7")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.8")
    debugImplementation("androidx.compose.ui:ui-tooling-preview:1.7.8")
    implementation("jp.wasabeef:blurry:4.0.1")


    // AndroidX libraries
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Google Material Design
    implementation("com.google.android.material:material:1.12.0")
    // Accompanist for additional Compose functionalities
    implementation("com.google.accompanist:accompanist-navigation-material:0.36.0")
    implementation("com.google.accompanist:accompanist-pager:0.36.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.36.0")
    implementation("com.googlecode.libphonenumber:libphonenumber:8.13.54")

    // Other libraries
    implementation("com.github.MikeOrtiz:TouchImageView:3.6")
    implementation("io.coil-kt:coil:2.7.0")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.12.1")
    implementation("com.airbnb.android:lottie:6.6.2")
    implementation("androidx.transition:transition:1.5.1")

    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // MaterialRatingBar dependency
    implementation("me.zhanghai.android.materialratingbar:library:1.4.0")

    // Kotlin and coroutines
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.1.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-analytics:22.2.0")
    implementation("com.google.firebase:firebase-database-ktx:21.0.0")
    implementation("com.google.firebase:firebase-analytics-ktx:22.2.0")
    implementation("com.google.firebase:firebase-storage:21.0.1")
    implementation("com.google.android.gms:play-services-basement:18.5.0")

    // Testing
    implementation("io.github.oneuiproject:icons:1.1.0")
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20250107")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.8")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest:1.7.8")
    implementation("com.github.lzyzsd:circleprogress:1.2.1")
    implementation("com.github.QuadFlask:colorpicker:0.0.15")
    implementation("com.github.cachapa:ExpandableLayout:2.9.2")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    //supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:3.1.1"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.ktor:ktor-client-android:3.0.3")
    implementation("io.github.jan-tennert.supabase:storage-kt")

    //ktor
    implementation("io.ktor:ktor-client-android:3.0.3")


}

