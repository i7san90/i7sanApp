# Add project specific ProGuard rules here.

# Room
-keep class androidx.room.** { * }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase

# Moshi / JSON serialization
-keepattributes *Annotation*,Signature,EnclosingMethod
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keepnames class * implements com.squareup.moshi.JsonAdapter
-keep class com.example.** { * }

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { * }
-dontwarn retrofit2.**

