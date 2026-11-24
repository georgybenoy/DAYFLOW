package com.dayflow.app.data.mapper

interface EntityMapper<Entity, Domain> {
    fun toDomain(entity: Entity): Domain
    fun toEntity(domain: Domain): Entity
}
