# JNI resolves native methods by class/method name; keep them stable.
-keepclasseswithmembernames class com.athea.app.engine.PtyBridge {
    native <methods>;
}

# kotlinx.serialization — keep serializable models (Journal, Storage).
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep @kotlinx.serialization.Serializable class **
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclasseswithmembers class ** {
    @kotlinx.serialization.Serializable <init>(...);
}
-dontwarn kotlinx.serialization.**

# JSch loads its crypto and key classes by name (for example com.jcraft.jsch.jce.KeyPairGenRSA),
# so R8 cannot see them and would strip them from the release build.
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**
