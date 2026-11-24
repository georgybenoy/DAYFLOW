# ==============================================================================
# DAYFLOW — R8 / ProGuard Configuration
# Offline-first personal productivity and execution app.
# ==============================================================================

# General Optimization & Code Quality
-dontwarn java.lang.invoke.**
-dontwarn sun.misc.Unsafe
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ------------------------------------------------------------------------------
# Kotlinx Serialization
# ------------------------------------------------------------------------------
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt

# Keep serializable classes and generated serializers
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * extends kotlinx.serialization.KSerializer {
    <init>(...);
    public static *** Companion;
}
-keepclassmembers class * {
    public static *** Companion;
}
-keepclassmembers class * {
    public static *** INSTANCE;
}
-keep class *$$serializer {
    public static *** INSTANCE;
}
-keepclasseswithmembers class * {
    public static *** serializer(...);
}

# Dayflow backup and persistence data models
-keep class com.dayflow.app.backup.model.** { *; }
-keep class com.dayflow.app.domain.model.** { *; }
-keep class com.dayflow.app.core.navigation.** { *; }

# ------------------------------------------------------------------------------
# Room Database & SQLite
# ------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.TypeConverter class * { *; }
-keep class com.dayflow.app.data.local.entity.** { *; }
-keep class com.dayflow.app.data.local.dao.** { *; }
-keep class com.dayflow.app.data.local.migrations.** { *; }

# ------------------------------------------------------------------------------
# Dagger & Hilt
# ------------------------------------------------------------------------------
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclassmembers,allowobfuscation class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
    @javax.inject.Inject <methods>;
}
-keep class * extends androidx.hilt.work.HiltWorker { *; }
-keep class androidx.hilt.work.** { *; }

# ------------------------------------------------------------------------------
# WorkManager
# ------------------------------------------------------------------------------
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.WorkerParameters { *; }
-keep class com.dayflow.app.backup.DailyBackupWorker { *; }

# ------------------------------------------------------------------------------
# Kotlin Coroutines & DataStore
# ------------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences { *; }

# ------------------------------------------------------------------------------
# Jetpack Compose
# ------------------------------------------------------------------------------
-keep class androidx.compose.ui.platform.AndroidComposeView { *; }
-keep class androidx.compose.runtime.** { *; }
