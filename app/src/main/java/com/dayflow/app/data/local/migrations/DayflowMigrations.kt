package com.dayflow.app.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DayflowMigrations {
    /**
     * Migration from Version 1 to Version 2.
     * Demonstrates safe, non-destructive schema evolution preserving all existing user records.
     * Note: fallbackToDestructiveMigration is strictly banned in DayFlow.
     */
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Non-destructive addition of optional metadata column with NULL default
            db.execSQL("ALTER TABLE categories ADD COLUMN note TEXT DEFAULT NULL")
        }
    }

    /**
     * Complete registry of all explicit database migrations.
     */
    val ALL_MIGRATIONS: Array<Migration> = arrayOf(
        MIGRATION_1_2
    )
}
