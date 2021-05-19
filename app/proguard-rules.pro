# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
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

-dontwarn android.support.v7.**
-keep class android.support.v7.* { *; }
-keep interface android.support.v7.* { *; }

# support design
-dontwarn android.support.design.**
-keep class android.support.design.* { *; }
-keep interface android.support.design.* { *; }
-keep public class android.support.design.R$* { *; }

-keep public class com.amirdaryabak.runningapp.ui.MainActivity
-keep public class com.amirdaryabak.runningapp.ui.LoginActivity
-keep class com.amirdaryabak.runningapp.ui.** { <fields>; }


-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
   long producerIndex;
   long consumerIndex;
}

-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

-keep class com.amirdaryabak.runningapp.models.*

# Consumer proguard rules for plugins

-dontwarn com.mapbox.mapboxandroiddemo.examples.plugins.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# MAS data models that will be serialized/deserialized over Gson
-keep class com.mapbox.services.api.directionsmatrix.v1.models.** { *; }

-keep class android.arch.lifecycle.** { *; }
-keep class com.mapbox.android.core.location.** { *; }
-keep class com.mapbox.mapboxsdk.** { *; }

# --- Java ---
-dontwarn java.awt.Color
-dontwarn com.mapbox.api.staticmap.v1.models.StaticMarkerAnnotation
-dontwarn com.mapbox.api.staticmap.v1.models.StaticPolylineAnnotation
-dontwarn com.sun.istack.internal.NotNull

# Mapbox
-keep class com.mapbox.android.telemetry.**
-keep class com.mapbox.android.core.location.**
-keep class android.arch.lifecycle.** { *; }
-keep class com.mapbox.android.core.location.** { *; }
-dontnote com.mapbox.mapboxsdk.**
-dontnote com.mapbox.android.gestures.**
-dontnote com.mapbox.mapboxsdk.plugins.**

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**