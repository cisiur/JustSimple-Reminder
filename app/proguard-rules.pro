# Keep Room entities
-keep class com.justsimple.reminder.data.db.** { *; }

# Keep RevenueCat
-keep class com.revenuecat.purchases.** { *; }

# Keep AdMob
-keep class com.google.android.gms.ads.** { *; }

# Keep Hilt-generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

