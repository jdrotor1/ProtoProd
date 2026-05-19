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
                if (entry is CatalogEntry.Parts) {
                    if (entry.data.manufacturer.isNotBlank()) bits.add("mfr=${entry.data.manufacturer}")
                    if (entry.data.mpn.isNotBlank()) bits.add("mpn=${entry.data.mpn}")
                    if (entry.data.unit_cost_usd > 0) bits.add("$${entry.data.unit_cost_usd}")
                    if (entry.data.lifecycle_status != "active") bits.add("status=${entry.data.lifecycle_status}")
                }
                "  - ${bits.joinToString(" · ")}"
            }
    }

    fun currentSelection(q: WizardQuestion): String = when (q) {
        is WizardQuestion.Text -> textOrPlaceholder(q.key)
        is WizardQuestion.Spec -> specOrPlaceholder(q.key)
        is WizardQuestion.CategoryQ -> categoryDetailed(q.catKey)
        is WizardQuestion.Review -> ""
    }

    fun optionsBlock(q: WizardQuestion): String = when (q) {
        is WizardQuestion.Text -> "(free-text input — no preset options)"
        is WizardQuestion.Spec -> q.options.joinToString("\n") { "- $it" }
        is WizardQuestion.CategoryQ -> {
            val entries = catalog.entries.filter { it.category == q.name }
            if (entries.isEmpty()) "(no entries in the embedded catalog for this category)"
            else entries.joinToString("\n") { entry ->
                val bits = mutableListOf("- ${entry.name}")
                if (entry is CatalogEntry.Parts) {
                    if (entry.data.manufacturer.isNotBlank()) bits.add("mfr=${entry.data.manufacturer}")
                    if (entry.data.mpn.isNotBlank()) bits.add("mpn=${entry.data.mpn}")
                    if (entry.data.unit_cost_usd > 0) bits.add("$${entry.data.unit_cost_usd}")
                    if (entry.data.lifecycle_status != "active") bits.add("status=${entry.data.lifecycle_status}")
                }
                bits.joinToString(" · ")
            }
        }
        is WizardQuestion.Review -> ""
    }

    fun selectionsSnapshot(): String = buildString {
        appendLine("## Selections snapshot (all 33 questions)")
        appendLine()
        appendLine("### Basics")
        listOf("project_name", "tooling", "connectivity_summary").forEach { key ->
            val label = (Questions.ALL.firstOrNull { it is WizardQuestion.Text && it.key == key } as? WizardQuestion.Text)?.label
                ?: key
            appendLine("- $label: ${textOrPlaceholder(key)}")
        }
        appendLine()
        appendLine("### Specs")
        Questions.ALL.filterIsInstance<WizardQuestion.Spec>().forEach { q ->
            appendLine("- ${q.label}: ${specOrPlaceholder(q.key)}")
        }
        appendLine()
        appendLine("### Components")
        Questions.ALL.filterIsInstance<WizardQuestion.CategoryQ>().forEach { q ->
            val sel = state.catSel[q.catKey].orEmpty()
            if (sel.isEmpty()) {
                appendLine("- ${q.name}: [none selected]")
            } else {
                appendLine("- ${q.name}:")
                appendLine(categoryDetailed(q.catKey))
            }
        }
    }
}
