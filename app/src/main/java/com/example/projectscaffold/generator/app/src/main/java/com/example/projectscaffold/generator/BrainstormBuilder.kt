package com.example.projectscaffold.generator

import com.example.projectscaffold.data.ParsedCatalog
import com.example.projectscaffold.model.Questions
import com.example.projectscaffold.model.WizardQuestion
import com.example.projectscaffold.model.WizardState

class BrainstormBuilder(
    state: WizardState,
    catalog: ParsedCatalog
) {
    private val h = PromptHelpers(state, catalog)

    fun brainstormPrompt(idx: Int): String = buildString {
        val q = Questions.ALL[idx]
        appendLine("# Brainstorm — Question ${idx + 1} of 33")
        appendLine()
        appendLine("I'm in a linear project-scaffold wizard for a new hardware/firmware/app product (prototype-to-product rigor).")
        appendLine()
        appendLine("## Current Question")
        appendLine(h.questionLabel(q))
        appendLine()
        appendLine("## Available Options")
        appendLine(h.optionsBlock(q))
        appendLine()
        appendLine("## My Current Selection (if any)")
        appendLine(h.currentSelection(q))
        appendLine()
        append(h.selectionsSnapshot())
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
        appendLine(h.questionLabel(q))
        appendLine()
        appendLine("## My Locked Selection for This Step")
        appendLine(h.currentSelection(q))
        appendLine()
        append(h.selectionsSnapshot())
        appendLine()
        appendLine()
        appendLine("## What To Build")
        appendLine("Create a file named `$stepFile` containing only this step's data. Use the heading `## Step ${idx + 1} — ${h.questionLabel(q)}`. Save via Claude's file-creation tools.")

        if (isLast) {
            appendLine()
            appendLine("## Final step: also assemble all 33 step files into the 17 scaffold files (memory.md, 00_brainstorm.md, 01_requirements.md, 02_design_mech.md, 02_design_elec.md, 02_design_fw.md, 02_design_app.md, 03_interfaces.md, 04_bom.csv, 05_derating.csv, 06_audit.md, 07_build.md, 08_test_plan.md, risks.md, changelog.md, sub_boms/README.md, sds/README.md). Save each via Claude's file-creation tools.")
        }
    }
}
