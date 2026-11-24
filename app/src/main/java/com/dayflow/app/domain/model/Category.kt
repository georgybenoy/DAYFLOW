package com.dayflow.app.domain.model

import java.time.Instant
import java.util.UUID

data class Category(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String,
    val iconName: String,
    val isDefault: Boolean = false,
    val createdAt: Instant = Instant.now()
) {
    companion object {
        val DEFAULT_CATEGORIES = listOf(
            Category(id = "cat_academics", name = "Academics", colorHex = "#76CDBB", iconName = "School", isDefault = true),
            Category(id = "cat_personal", name = "Personal", colorHex = "#A5E6D6", iconName = "Person", isDefault = true),
            Category(id = "cat_fitness", name = "Fitness", colorHex = "#F0E99B", iconName = "FitnessCenter", isDefault = true),
            Category(id = "cat_work", name = "Work", colorHex = "#DCC3F5", iconName = "Work", isDefault = true),
            Category(id = "cat_projects", name = "Projects", colorHex = "#B99DD8", iconName = "Assignment", isDefault = true),
            Category(id = "cat_finance", name = "Finance", colorHex = "#F1C879", iconName = "AccountBalance", isDefault = true),
            Category(id = "cat_other", name = "Other", colorHex = "#A5B6AD", iconName = "Category", isDefault = true)
        )
    }
}
