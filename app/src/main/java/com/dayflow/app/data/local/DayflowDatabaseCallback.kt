package com.dayflow.app.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dayflow.app.domain.model.Category

class DayflowDatabaseCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        seedCategories(db)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        // Ensure default categories exist (idempotent fallback)
        seedCategories(db)
    }

    private fun seedCategories(db: SupportSQLiteDatabase) {
        db.beginTransaction()
        try {
            val nowMillis = System.currentTimeMillis()
            val stmt = db.compileStatement(
                """
                INSERT OR IGNORE INTO categories (id, name, colorHex, iconName, isDefault, createdAt)
                VALUES (?, ?, ?, ?, 1, ?)
                """.trimIndent()
            )

            Category.DEFAULT_CATEGORIES.forEach { category ->
                stmt.bindString(1, category.id)
                stmt.bindString(2, category.name)
                stmt.bindString(3, category.colorHex)
                stmt.bindString(4, category.iconName)
                stmt.bindLong(5, nowMillis)
                stmt.executeInsert()
                stmt.clearBindings()
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
