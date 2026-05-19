package com.example.projectscaffold.generator

import com.example.projectscaffold.data.ParsedCatalog
import com.example.projectscaffold.model.CatalogEntry
import com.example.projectscaffold.model.Questions
import com.example.projectscaffold.model.WizardQuestion
import com.example.projectscaffold.model.WizardState

class PromptBuilders(
    private val state: WizardState,
    private val catalog: ParsedCatalog
) {

    fun brainstormPrompt(idx: Int): String = buildString {
        val q = Questions.ALL[idx]
        appendLine("# Brainstorm — Question ${idx + 1} of 33")
        appendLine()
        appendLine("I'm in a linear project-scaffold wizard for a new hardware/firmware/app product (prototype-to-product rigor).")
        appendLine()
        appendLine("## Current Question")
        appendLine(questionLabel(q))
        appendLine()
        appendLine("## Available Options")
        appendLine(optionsBlock(q))
        appendLine()
        appendLine("## My Current Selection (if any)")
        appendLine(currentSelection(q))
        appendLine()
        append(selectionsSnapshot())
        appendLine()
        appendLine()
        appendLine("## What I Need From You")
        appendLine("1. Critique each available option against my project context above. Be specific.")
        appendLine("2. Rank concerns by severity (blocking / serious / nuisance).")
        appendLine("3. Recommend the best option for my context.")
        appendLine("4. If you suggest items not in my Available Options list, present them as a JSON block matching the catalog schema, then a markdown table.")
    }

    fun buildPrompt(idx: Int): String = buildString {
        val q = Questions.ALL[idx]
        val key = when (q) {
            is WizardQuestion.Text -> q.key
            is WizardQuestion.Spec -> q.key
            is WizardQuestion.CategoryQ -> q.catKey.lowercase()
            is WizardQuestion.Review -> "review"
        }
        val stepFile = "step_${"%02d".format(idx + 1)}_${key}.md"
        val isLast = idx == Questions.LAST_BUILD_STEP_IDX

        appendLine("# Build — Question ${idx + 1} of 33")
        appendLine()
        appendLine("## Current Question")
        appendLine(questionLabel(q))
        appendLine()
        appendLine("## My Locked Selection for This Step")
        appendLine(currentSelection(q))
        appendLine()
        append(selectionsSnapshot())
        appendLine()
        appendLine()
        appendLine("## What To Build")
        appendLine("Create a file named `$stepFile` containing only this step's data, formatted as a structured markdown block. Use the heading `## Step ${idx + 1} — ${questionLabel(q)}`. Save via Claude's file-creation tools.")

        if (isLast) {
            appendLine()
            appendLine("## Final step: also assemble all 33 step files into the 17 scaffold files (memory.md, 00_brainstorm.md, 01_requirements.md, 02_design_mech.md, 02_design_elec.md, 02_design_fw.md, 02_design_app.md, 03_interfaces.md, 04_bom.csv, 05_derating.csv, 06_audit.md, 07_build.md, 08_test_plan.md, risks.md, changelog.md, sub_boms/README.md, sds/README.md). Save each via Claude's file-creation tools.")
        }
    }

    fun auditPrompt(): String = buildString {
        appendLine("# System-Wide Audit — Prototype Phase")
        appendLine()
        appendLine("All 17 scaffold files have been assembled. Run Rule-4 audit.")
        appendLine()
        append(selectionsSnapshot())
        appendLine()
        appendLine()
        appendLine("## What To Audit")
        appendLine("Read all 17 files. Produce a numbered findings report. Be straight and honest.")
        appendLine()
        appendLine("1. Cross-file consistency (pinmap elec↔fw↔interfaces, protocols fw↔app, BOM ↔ design files, diagnostic interfaces have test points + build flags, memory.md ORIGINAL SELECTIONS matches actuals).")
        appendLine("2. Completeness (no placeholders, every section has substance, requirement IDs populated, test plan has tests per req).")
        appendLine("3. Electrical derating system-wide (every elec part in BOM has a 05_derating.csv row, all params filled, flag pass_fail=fail, verify ≤80% V / 50% P / 70% I commercial defaults).")
        appendLine("4. Power budget reconciliation (sum worst-case current per rail vs supply capacity, flag rails ≥80% loaded).")
        appendLine("5. Thermal feasibility (parts >0.5W have thermal path in mech design, ambient aligns with operating environment).")
        appendLine("6. Pinmap closure (every elec-assigned MCU pin assigned in fw, no double-drives, resets/boot/debug pins present).")
        appendLine("7. Protocol completeness (fw↔app protocol parity, packet schema defined in 03_interfaces.md).")
        appendLine("8. Memory budget (fw flash + RAM vs MCU available, flag <20% headroom).")
        appendLine("9. Software-hardware contract (every fw-driven peripheral has a BOM part, every hw peripheral has fw support).")
        appendLine("10. Stale-part flag for SELECTED components only (last_verified_date >90 days = STALE, ignore unselected catalog).")
        appendLine()
        appendLine("Do NOT audit DFM/DFA exit gates (prototype phase). Do NOT flag missing EMC test reports etc unless I asked for that phase.")
        appendLine()
        appendLine("Output: severity (BLOCKING/SERIOUS/NUISANCE), files affected, description, concrete fix. End with count summary and one-sentence verdict.")
    private fun questionLabel(q: WizardQuestion): String = when (q) {
        is WizardQuestion.Text -> q.label
        is WizardQuestion.Spec -> q.label
        is WizardQuestion.CategoryQ -> q.name
        is WizardQuestion.Review -> "Review"
    }

    private fun textOrPlaceholder(key: String): String {
        val v = state.texts[key]?.trim().orEmpty()
        return if (v.isEmpty()) "[not specified]" else v
    }

    private fun specOrPlaceholder(key: String): String {
        val sel = state.specSel[key] ?: return "[not specified]"
        if (sel.isEmpty()) return "[not specified]"
        return sel.joinToString(", ") { if (it.startsWith("UD:")) it.removePrefix("UD:") else it }
    }

    private fun categoryDetailed(catKey: String): String {
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
                    if (entry.data.unit_
