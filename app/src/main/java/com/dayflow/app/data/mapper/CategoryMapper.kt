package com.dayflow.app.data.mapper

import com.dayflow.app.data.local.entity.CategoryEntity
import com.dayflow.app.domain.model.Category

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    colorHex = colorHex,
    iconName = iconName,
    isDefault = isDefault,
    createdAt = createdAt
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    colorHex = colorHex,
    iconName = iconName,
    isDefault = isDefault,
    createdAt = createdAt
)
