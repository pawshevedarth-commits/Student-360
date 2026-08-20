# Gson optimization keep-rules
-keep class com.google.gson.** { *; }
-keep class com.student360.app.service.BackupRestoreManager$BackupData { *; }
-keep class com.student360.app.data.local.entity.** { *; }

# Room optimization keep-rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
