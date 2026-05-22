# ── Hilt ──────────────────────────────────────────────────────────────────────
# 1. Keep classes that carry Hilt annotations (the ones you write)
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }

# 2. Keep all dagger.hilt.** library classes
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# 3. Keep Hilt-generated WRAPPER classes (Hilt_JustSimpleReminderApp, Hilt_MainActivity …)
#    These live in the APP package and carry NO annotation themselves — the annotation
#    rules above won't catch them. R8 can rename/remove them without this rule.
-keep class **.Hilt_* { *; }

# 4. Keep Dagger-generated component implementations (DaggerAppName_HiltComponents_*)
-keep class ** implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class ** implements dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }

# 5. Keep @Inject constructors — Hilt resolves bindings by calling them at runtime
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}
-keep class javax.inject.** { *; }

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class com.justsimple.reminder.data.db.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }

# ── RevenueCat ────────────────────────────────────────────────────────────────
-keep class com.revenuecat.purchases.** { *; }
-dontwarn com.revenuecat.purchases.**

# ── AdMob ─────────────────────────────────────────────────────────────────────
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# ── Kotlin / Coroutines ───────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keep class kotlin.coroutines.** { *; }
-dontwarn kotlin.**
-dontnote kotlinx.serialization.AnnotationsKt
