package com.dayflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val isDefault: Boolean = false,
    val createdAt: Instant = Instant.now()
)
