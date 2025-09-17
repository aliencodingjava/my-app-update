######################################
# ✅ CORE FRAGMENTS (only what you use)
######################################
-keep class com.flights.studio.RateUsDialogFragment
-keep class com.flights.studio.FeedbackBottomSheet

######################################
# ✅ BottomSheet base (safe)
######################################
-keep class com.google.android.material.bottomsheet.BottomSheetDialogFragment
-dontwarn com.google.android.material.**

######################################
# ✅ Jetpack Compose (consumer rules exist; just quiet warnings)
######################################
-dontwarn androidx.compose.**
-dontwarn kotlin.**
-dontwarn org.jetbrains.annotations.**

######################################
# ✅ LiquidGlass (avoid any reflection stripping)
######################################
-keep class com.kyant.liquidglass.** { *; }
-dontwarn com.kyant.liquidglass.**

######################################
# ✅ Glide (v4) — minimal & correct
######################################
# Keep the generated module & your AppGlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule
-keep class * extends com.bumptech.glide.module.LibraryGlideModule

# Common enums used via reflection
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { *; }

# Basic classes + silence
-keep class com.bumptech.glide.Glide
-keep class com.bumptech.glide.RequestManager
-dontwarn com.bumptech.glide.**

######################################
# ✅ BigImageViewer (and its Glide loader)
######################################
-keep class com.github.piasy.** { *; }
-dontwarn com.github.piasy.**

######################################
# ✅ (Optional) Coil (if you use it anywhere)
######################################
-dontwarn coil.**
-dontwarn coil3.**

######################################
# ✅ Firebase (only if reflection used)
######################################
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }

######################################
# ✅ Ktor & Supabase
######################################
-dontwarn io.ktor.**
-dontwarn io.github.jan.supabase.**

######################################
# ✅ Kotlin Serialization
######################################
-dontwarn kotlinx.serialization.**
-keep class kotlinx.serialization.** { *; }

######################################
# ✅ View XML onClick methods
######################################
-keepclassmembers class * {
    public void *(android.view.View);
}

######################################
# ✅ Parcelable
######################################
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

######################################
# ✅ Reflection / annotations
######################################
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
}

######################################
# ✅ Logging strip (safe & effective for APK size)
######################################
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}
