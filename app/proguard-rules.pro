# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Gson rules
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Ensure @Keep annotations are always honored
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Weather Data models for Gson
-keep class com.sameerasw.overcast.weather.model.** { *; }
-keepclassmembers class com.sameerasw.overcast.weather.model.** { *; }

# Keep ViewModel constructors for reflection-based instantiation
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# Ensure anonymous TypeToken subclasses (used for GSON generic lists) are kept
-keepclassmembers class * extends com.google.gson.reflect.TypeToken {
    protected <init>(...);
}
