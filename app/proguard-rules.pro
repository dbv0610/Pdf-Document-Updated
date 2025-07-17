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

-keep class com.shockwave.**
-keep class com.aspose.** { *; }
-dontwarn com.aspose.**

-dontwarn com.tom_roush.pdfbox.filter.JPXFilter
-dontwarn com.gemalto.jp2.**
-assumenosideeffects class com.tom_roush.pdfbox.filter.JPXFilter {
    *;
}
-keep class androidx.room.** { *; }
-keep interface androidx.room.* { *; }

# Giữ Entity, Dao, Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

-keep class com.azg.pdf8.model.** { *; }
-keep class com.azg.pdf8.database.** { *; }
-keep class com.documentreader.manage.pdfreader.viewpdf.open.model.** { *; }
-keep class com.documentreader.manage.pdfreader.viewpdf.open.database.** { *; }
-keep class com.wxiwei.office.** { *; }

# Giữ các lớp TypeToken và các lớp liên quan
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keep class * implements java.lang.reflect.Type { *; }

-keep @androidx.annotation.Keep class * { *; }

-dontwarn com.gemalto.jp2.**
-keep class com.tom_roush.pdfbox.filter.JPXFilter { *; }

-keep class android.util.Log {
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}