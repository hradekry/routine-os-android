# Add project specific ProGuard rules here.
-keep class com.routineos.data.models.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
