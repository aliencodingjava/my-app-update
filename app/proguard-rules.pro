# --- Preserve RateUsDialogFragment and other related fragments ---
-keep class com.flights.studio.RateUsDialogFragment { *; }
-keep class com.flights.studio.FeedbackDialogFragment { *; }

# --- Preserve Material Design components ---
-keep class com.google.android.material.bottomsheet.BottomSheetDialogFragment { *; }
-keep class com.google.android.material.** { *; }

# --- Preserve DonutProgress and custom animations ---
-keep class com.github.lzyzsd.circleprogress.DonutProgress { *; }

# --- Preserve Firebase database references and models ---
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# --- Preserve annotations used in your project ---
-keep class android.annotation.SuppressLint
-dontwarn android.annotation.SuppressLint

# --- Preserve methods and fields referenced in XML layouts ---
-keepclassmembers class * {
    @android.view.View$OnClickListener <methods>;
}

# --- General rules to avoid stripping dynamically used methods/classes ---
-keepclassmembers class ** {
    *;
}

# --- Preserve Parcelable classes (if used in arguments) ---
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# --- Avoid warnings for missing classes ---
-dontwarn com.google.android.material.**
-dontwarn com.google.firebase.**
-dontwarn javax.annotation.**

# --- Debugging ProGuard Issues (optional logging tools) ---
-keep class android.util.Log { *; }
