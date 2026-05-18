package com.example.projectscaffold.generator

import com.example.projectscaffold.data.ParsedCatalog
import com.example.projectscaffold.model.CatalogEntry
import com.example.projectscaffold.model.Questions
import com.example.projectscaffold.model.WizardQuestion
import com.example.projectscaffold.model.WizardState

/**
 * Ports the prompt builders from the v3 PWA (JS) to Kotlin.
 * Three public entry points: brainstormPrompt(idx), buildPrompt(idx), auditPrompt().
 */
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
        appendLine("1. Critique each available option against my project context above. Be specific — production volume, BOM cost target, environment, EMC class, and any other selections that bear on this choice.")
        appendLine("2. Rank concerns by severity (blocking / serious / nuisance).")
        appendLine("3. Recommend the best option for my context. If [User Defined] is the right call, tell me what to type.")
        appendLine("4. If you suggest any components, formats, or options that aren't in my Available Options list above, present them at the end in TWO formats:")
        appendLine("   - First a JSON block matching this schema (drop-in pasteable into my catalog.json):")
        appendLine("     ```json")
        appendLine("     { \"id\": \"...\", \"category\": \"...\", \"name\": \"...\", \"manufacturer\": \"...\", \"mpn\": \"...\", \"supplier\": \"...\", \"supplier_url\": \"...\", \"unit_cost_usd\": 0, \"datasheet_url\": \"...\", \"sds_required\": false, \"lifecycle_status\": \"active\", \"last_verified_date\": \"YYYY-MM-DD\", \"notes\": \"...\", \"tags\": [] }")
        appendLine("     ```")
        appendLine("   - Then a markdown table of the same items for human review.")
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
        appendLine("I'm in a linear project-scaffold wizard. Treat this as a build step (per Rule 4, audit first if anything seems off; otherwise proceed).")
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
        appendLine("Create a file named `$stepFile` containing **only** this step's data, formatted as a structured markdown block ready to be merged into the final scaffold files. Use the heading `## Step ${idx + 1} — ${questionLabel(q)}` and a clean bulleted/tabular body with the selection and any relevant metadata (manufacturer/MPN/price/notes for catalog entries).")
        appendLine()
        appendLine("Use Claude's file-creation tools — save the file and give me a download link.")

        if (isLast) {
            appendLine()
            appendLine("## Additional task on this final build step")
            appendLine("After writing `$stepFile`, also assemble all 33 `step_NN_*.md` files into the 17 final scaffold files:")
            appendLine()
            appendLine("```")
            appendLine("memory.md")
            appendLine("00_brainstorm.md")
            appendLine("01_requirements.md")
            appendLine("02_design_mech.md")
            appendLine("02_design_elec.md")
            appendLine("02_design_fw.md")
            appendLine("02_design_app.md")
            appendLine("03_interfaces.md")
            appendLine("04_bom.csv")
            appendLine("05_derating.csv")
            appendLine("06_audit.md")
            appendLine("07_build.md")
            appendLine("08_test_plan.md")
            appendLine("risks.md")
            appendLine("changelog.md")
            appendLine("sub_boms/README.md")
            appendLine("sds/README.md")
            appendLine("```")
            appendLine()
            appendLine("Each file should follow the scaffold template structure that this project already documents (role tag on line 1, section headers, exit-gate checkboxes where defined, BOM/derating CSVs populated from selected components).")
            appendLine()
            appendLine("Save each via Claude's file-creation tools and give me download links for all 17.")
        }
    }

    fun auditPrompt(): String = buildString {
        appendLine("# System-Wide Audit — Prototype Phase")
        appendLine()
        appendLine("All 17 scaffold files have been assembled. Run Rule-4 audit before I move forward.")
        appendLine()
        append(selectionsSnapshot())
        appendLine()
        appendLine()
        appendLine("## What To Audit")
        appendLine("Read all 17 assembled files. Produce a numbered findings report covering these checks. Be straight and honest — don't soften severity to spare my feelings.")
        appendLine()
        appendLine("### 1. Cross-file consistency")
        appendLine("- Pinmap in `02_design_elec.md` matches `02_design_fw.md` and `03_interfaces.md`")
        appendLine("- Comms protocols in `02_design_fw.md` match `02_design_app.md`")
        appendLine("- Every component in `04_bom.csv` is referenced in the appropriate design file")
        appendLine("- Every diagnostic interface declared has test points called out in elec design and build flags in fw design")
        appendLine("- `memory.md` ORIGINAL SELECTIONS matches the actual selections used in design files")
        appendLine()
        appendLine("### 2. Completeness")
        appendLine("- No `[FROM_WIZARD]`, `[not specified]`, `[none selected]` placeholders left in places that need content")
        ap
