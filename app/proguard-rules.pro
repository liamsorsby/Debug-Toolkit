# Room instantiates WorkManager's generated database via its no-argument constructor.
-keep class androidx.work.impl.WorkDatabase_Impl {
    public <init>();
}

-keep class com.newrelic.** { *; }
-dontwarn com.newrelic.**
-keepattributes Exceptions, Signature, InnerClasses, LineNumberTable, SourceFile, EnclosingMethod

##
## NewRelic Gradle plugin 7.x may require the following additions:
##
# Retain generic signatures of TypeToken and its subclasses if R8 version 3.0 full-mode is enabled.
# https://r8.googlesource.com/r8/+/refs/heads/master/compatibility-faq.md#r8-full-mode
-keepattributes Signature
-keep class com.newrelic.com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.newrelic.com.google.gson.reflect.TypeToken
# For using GSON @Expose annotation
-keepattributes *Annotation*

-keep class com.google.ads.mediation.admob.AdMobAdapter {
    *;
}