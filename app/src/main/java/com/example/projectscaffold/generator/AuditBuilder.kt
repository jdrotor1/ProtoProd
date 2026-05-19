package com.example.projectscaffold.generator

import com.example.projectscaffold.data.ParsedCatalog
import com.example.projectscaffold.model.WizardState

class AuditBuilder(
    state: WizardState,
    catalog: ParsedCatalog
) {
    private val h = PromptHelpers(state, catalog)

    fun auditPrompt(): String = buildString {
        appendLine("# System-Wide Audit — Prototype Phase")
        appendLine()
        appendLine("All 17 scaffold files have been assembled. Run Rule-4 audit.")
        appendLine()
        append(h.selectionsSnapshot())
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
    }
}
