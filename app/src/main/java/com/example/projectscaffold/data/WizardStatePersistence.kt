package com.example.projectscaffold.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.projectscaffold.model.WizardState
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.wizardDataStore by preferencesDataStore(name = "wizard_state_v3")

@Serializable
private data class WizardSnapshot(
    val idx: Int = 0,
    val texts: Map<String, String> = emptyMap(),
    val catSel: Map<String, List<String>> = emptyMap(),
    val specSel: Map<String, List<String>> = emptyMap(),
    val udText: Map<String, String> = emptyMap()
)

class WizardStatePersistence(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("snapshot_v3")

    suspend fun save(state: WizardState) {
        val snap = WizardSnapshot(
            idx = state.idx,
            texts = state.texts,
            catSel = state.catSel.mapValues { it.value.toList() },
            specSel = state.specSel.mapValues { it.value.toList() },
            udText = state.udText
        )
        context.wizardDataStore.edit { it[key] = json.encodeToString(snap) }
    }

    suspend fun load(): WizardState? {
        val raw = context.wizardDataStore.data.first()[key] ?: return null
        return try {
            val s = json.decodeFromString<WizardSnapshot>(raw)
            WizardState(
                idx = s.idx,
                texts = s.texts,
                catSel = s.catSel.mapValues { it.value.toSet() },
                specSel = s.specSel.mapValues { it.value.toSet() },
                udText = s.udText
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun clear() {
        context.wizardDataStore.edit { it.remove(key) }
    }

    suspend fun hasMeaningfulState(): Boolean {
        val s = load() ?: return false
        return s.texts.values.any { it.isNotBlank() } ||
            s.catSel.values.any { it.isNotEmpty() } ||
            s.specSel.values.any { it.isNotEmpty() }
    }
}
