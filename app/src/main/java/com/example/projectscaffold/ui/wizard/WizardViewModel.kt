package com.example.projectscaffold.ui.wizard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectscaffold.data.CatalogRepository
import com.example.projectscaffold.data.ParsedCatalog
import com.example.projectscaffold.data.WizardStatePersistence
import com.example.projectscaffold.generator.PromptBuilders
import com.example.projectscaffold.model.Questions
import com.example.projectscaffold.model.WizardQuestion
import com.example.projectscaffold.model.WizardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WizardViewModel(app: Application) : AndroidViewModel(app) {

    private val catalogRepo = CatalogRepository(app)
    private val persistence = WizardStatePersistence(app)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val hasPartial = persistence.hasMeaningfulState()
            _ui.update { it.copy(hasResumableState = hasPartial) }
            try {
                val catalog = catalogRepo.loadCatalog()
                _ui.update { it.copy(catalog = catalog, catalogError = null) }
            } catch (e: Exception) {
                _ui.update { it.copy(catalogError = e.message ?: "Failed to load catalog") }
            }
        }
    }

    fun resumeOrDiscard(resume: Boolean) {
        viewModelScope.launch {
            if (resume) {
                persistence.load()?.let { saved ->
                    _ui.update { it.copy(state = saved, hasResumableState = false) }
                }
            } else {
                persistence.clear()
                _ui.update { it.copy(state = WizardState(), hasResumableState = false) }
            }
        }
    }

    fun updateText(key: String, value: String) {
        _ui.update { s -> s.copy(state = s.state.copy(texts = s.state.texts + (key to value))) }
        autoSave()
    }

    fun toggleCategory(catKey: String, entryId: String, multi: Boolean) {
        _ui.update { s ->
            val current = s.state.catSel[catKey].orEmpty()
            val next = when {
                entryId in current -> current - entryId
                multi -> current + entryId
                else -> setOf(entryId)
            }
            s.copy(state = s.state.copy(catSel = s.state.catSel + (catKey to next)))
        }
        autoSave()
    }

    fun toggleSpec(key: String, value: String, multi: Boolean, udText: String = "") {
        _ui.update { s ->
            val current = s.state.specSel[key].orEmpty()
            val newUd = s.state.udText + (key to udText.ifEmpty { s.state.udText[key].orEmpty() })

            val isUD = value == "[User Defined]"
            val next = when {
                isUD && multi -> {
                    val hadUd = current.any { it.startsWith("UD:") }
                    val cleared = current.filterNot { it.startsWith("UD:") }.toSet()
                    if (hadUd) cleared else cleared + "UD:${udText.trim()}"
                }
                isUD && !multi -> setOf("UD:${udText.trim()}")
                multi && value in current -> current - value
                multi -> current + value
                else -> setOf(value)
            }
            s.copy(state = s.state.copy(specSel = s.state.specSel + (key to next), udText = newUd))
        }
        autoSave()
    }

    fun updateUdText(key: String, text: String) {
        _ui.update { s ->
            val current = s.state.specSel[key].orEmpty().filterNot { it.startsWith("UD:") }.toSet() + "UD:${text.trim()}"
            s.copy(
                state = s.state.copy(
                    specSel = s.state.specSel + (key to current),
                    udText = s.state.udText + (key to text)
                )
            )
        }
        autoSave()
    }

    fun next() {
        val s = _ui.value
        val q = Questions.ALL[s.state.idx]
        if (q is WizardQuestion.Text && q.required) {
            if (s.state.texts[q.key].orEmpty().isBlank()) {
                _ui.update { it.copy(transientError = "This field is required.") }
                return
            }
        }
        val newIdx = (s.state.idx + 1).coerceAtMost(Questions.TOTAL - 1)
        _ui.update { it.copy(state = it.state.copy(idx = newIdx), transientError = null) }
        autoSave()
    }

    fun back() {
        _ui.update { it.copy(state = it.state.copy(idx = (it.state.idx - 1).coerceAtLeast(0))) }
        autoSave()
    }

    fun reset() {
        _ui.update { it.copy(state = WizardState()) }
        viewModelScope.launch { persistence.clear() }
    }

    fun consumeTransientError() {
        _ui.update { it.copy(transientError = null) }
    }

    fun prompts(): PromptBuilders? {
        val cat = _ui.value.catalog ?: return null
        return PromptBuilders(_ui.value.state, cat)
    }

    private fun autoSave() {
        viewModelScope.launch { persistence.save(_ui.value.state) }
    }
}

data class UiState(
    val state: WizardState = WizardState(),
    val hasResumableState: Boolean = false,
    val catalog: ParsedCatalog? = null,
    val catalogError: String? = null,
    val transientError: String? = null
)
