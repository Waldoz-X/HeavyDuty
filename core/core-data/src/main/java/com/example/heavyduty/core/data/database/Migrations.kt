package com.example.heavyduty.core.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema upgrades that keep the user's data. Every future version bump needs one here: the
 * database no longer falls back to wiping itself on upgrade (see DatabaseModule), because that
 * silently deleted every workout, routine and weigh-in the user had logged.
 */
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // English catalog name, kept for search once `name` holds the Spanish one.
        db.execSQL("ALTER TABLE exercises ADD COLUMN nameEn TEXT NOT NULL DEFAULT ''")
        // kg/lb per routine exercise (the machine used there) and per logged set.
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN weightUnit TEXT")
        db.execSQL("ALTER TABLE workout_sets ADD COLUMN weightUnit TEXT")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // English instructions, shown when the app is in English. Filled from the catalog on
        // the next open (see SeedData.localizeExerciseInstructions).
        db.execSQL("ALTER TABLE exercises ADD COLUMN instructionsEn TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Indices only — no data changes. Names follow Room's index_<table>_<columns> scheme.
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_startTime` ON `workout_sessions` (`startTime`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_status_startTime` ON `workout_sessions` (`status`, `startTime`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_exerciseId_completedAt` ON `workout_sets` (`exerciseId`, `completedAt`)")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18)
