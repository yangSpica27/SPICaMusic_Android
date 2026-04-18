# Room only needs the database entry point preserved for generated lookups.
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
