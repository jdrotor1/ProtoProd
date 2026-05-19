package com.example.projectscaffold.generator

import com.example.projectscaffold.data.ParsedCatalog
import com.example.projectscaffold.model.CatalogEntry
import com.example.projectscaffold.model.Questions
import com.example.projectscaffold.model.WizardQuestion
import com.example.projectscaffold.model.WizardState

internal class PromptHelpers(
    val state: WizardState,
    val catalog: ParsedCatalog
) {
    fun questionLabel(q: WizardQuestion): String = when (q) {
        is WizardQuestion.Text -> q.label
        is WizardQuestion.Spec -> q.label
        is WizardQuestion.CategoryQ -> q.name
        is WizardQuestion.Review -> "Review"
    }

    fun textOrPlaceholder(key: String): String {
        val v = state.texts[key]?.trim().orEmpty()
        return if (v.isEmpty()) "[not specified]" else v
    }

    fun specOrPlaceholder(key: String): String {
        val sel = state.specSel[key] ?: return "[not specified]"
        if (sel.isEmpty()) return "[not specified]"
        return sel.joinToString(", ") { if (it.startsWith("UD:")) it.removePrefix("UD:") else it }
    }

    fun categoryDetailed(catKey: String): String {
        val q = Questions.ALL.firstOrNull { it is WizardQuestion.CategoryQ && it.catKey == catKey }
            as? WizardQuestion.CategoryQ ?: return "[unknown category]"
        val sel = state.catSel[catKey].orEmpty()
        if (sel.isEmpty()) return "[none selected]"
        return catalog.entries
            .filter { it.category == q.name && it.id in sel }
            .joinToString("\n") { entry ->
                val bits = mutableListOf(entry.name)
