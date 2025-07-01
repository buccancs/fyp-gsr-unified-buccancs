# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep CameraX classes
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Keep Shimmer SDK classes
-keep class com.shimmerresearch.** { *; }
-keep class com.shimmersensing.** { *; }
-dontwarn com.shimmerresearch.**
-dontwarn com.shimmersensing.**

# Keep Topdon SDK classes (adjust package name as needed)
-keep class com.topdon.** { *; }
-dontwarn com.topdon.**

# Keep networking classes
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep serialization classes
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Keep USB and Bluetooth related classes
-keep class android.hardware.usb.** { *; }
-keep class android.bluetooth.** { *; }

# Keep sensor and camera related classes
-keep class android.hardware.camera2.** { *; }
-keep class android.media.** { *; }