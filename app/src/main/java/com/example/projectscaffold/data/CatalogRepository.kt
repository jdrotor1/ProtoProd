package com.example.projectscaffold.data

import android.content.Context
import com.example.projectscaffold.Constants
import com.example.projectscaffold.model.Catalog
import com.example.projectscaffold.model.CatalogEntry
import com.example.projectscaffold.model.NonPartsComponent
import com.example.projectscaffold.model.PartsComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CatalogRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun loadCatalog(): ParsedCatalog = withContext(Dispatchers.IO) {
        val raw = context.assets.open(Constants.BUNDLED_CATALOG_ASSET)
            .bufferedReader().use { it.readText() }
        parseAndPartition(raw)
    }

    private fun parseAndPartition(raw: String): ParsedCatalog {
        val catalog = json.decodeFromString<Catalog>(raw)
        val entries = catalog.components.mapNotNull { element ->
            val obj = element.jsonObject
            val category = obj["category"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val isNonParts = category == "Comms-Protocol" || category == "EDA / Netlist Format"
            try {
                if (isNonParts) {
                    CatalogEntry.NonParts(json.decodeFromJsonElement(NonPartsComponent.serializer(), element))
                } else {
                    CatalogEntry.Parts(json.decodeFromJsonElement(PartsComponent.serializer(), element))
                }
            } catch (e: Exception) {
                null
            }
        }
        return ParsedCatalog(
            catalogVersion = catalog.catalog_version,
            schemaVersion = catalog.schema_version,
            lastUpdated = catalog.last_updated,
            entries = entries
        )
    }

    fun isStale(entry: CatalogEntry): Boolean {
        if (entry !is CatalogEntry.Parts) return false
        val dateStr = entry.data.last_verified_date.ifBlank { return true }
        return try {
            val date = LocalDate.parse(dateStr)
            ChronoUnit.DAYS.between(date, LocalDate.now()) > Constants.STALE_THRESHOLD_DAYS
        } catch (e: Exception) {
            true
        }
    }
}

data class ParsedCatalog(
    val catalogVersion: String,
    val schemaVersion: String,
    val lastUpdated: String,
    val entries: List<CatalogEntry>
)
