# Storage module ProGuard rules
-keep class me.spica27.spicamusic.storage.api.** { *; }
-keep class me.spica27.spicamusic.storage.impl.entity.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
