package com.example.projectscaffold.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Catalog(
    val catalog_version: String,
    val schema_version: String,
    val last_updated: String,
    val components: List<JsonElement>
)

@Serializable
data class PartsComponent(
    val id: String,
    val category: String,
    val name: String,
    val manufacturer: String = "",
    val mpn: String = "",
    val supplier: String = "",
    val supplier_url: String = "",
    val unit_cost_usd: Double = 0.0,
    val datasheet_url: String = "",
    val sds_required: Boolean = false,
    val lifecycle_status: String = "active",
    val last_verified_date: String = "",
    val notes: String = "",
    val tags: List<String> = emptyList()
)

@Serializable
data class NonPartsComponent(
    val id: String,
    val category: String,
    val name: String,
    val notes: String = ""
)

sealed class CatalogEntry {
    abstract val id: String
    abstract val category: String
    abstract val name: String

    data class Parts(val data: PartsComponent) : CatalogEntry() {
        override val id get() = data.id
        override val category get() = data.category
        override val name get() = data.name
    }

    data class NonParts(val data: NonPartsComponent) : CatalogEntry() {
        override val id get() = data.id
        override val category get() = data.category
        override val name get() = data.name
    }
}
