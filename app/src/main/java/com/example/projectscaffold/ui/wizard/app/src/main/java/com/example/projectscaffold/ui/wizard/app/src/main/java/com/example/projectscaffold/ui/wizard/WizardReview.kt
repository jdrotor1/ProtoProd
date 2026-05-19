package com.example.projectscaffold.ui.wizard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.projectscaffold.model.Questions
import com.example.projectscaffold.model.WizardQuestion

@Composable
fun ReviewContent(vm: WizardViewModel) {
    val ui by vm.ui.collectAsState()
    val ctx = LocalContext.current

    Spacer(Modifier.height(8.dp))
    Text("Build Summary", style = MaterialTheme.typography.titleLarge)
    Text(
        "All 33 step prompts have been collected. By the end of Q33's build prompt, Claude has produced and assembled the 17 scaffold files. Use the audit button below to verify the assembled set.",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
    )

    ReviewCard("Basics") {
        listOf("project_name", "tooling", "connectivity_summary").forEach { key ->
            val label = (Questions.ALL.firstOrNull { it is WizardQuestion.Text && it.key == key } as? WizardQuestion.Text)?.label.orEmpty()
            val v = ui.state.texts[key]?.trim().orEmpty().ifBlank { "—" }
            ReviewRow(label, v)
        }
    }

    ReviewCard("Specs") {
        Questions.ALL.filterIsInstance<WizardQuestion.Spec>().forEach { sq ->
            val sel = ui.state.specSel[sq.key].orEmpty()
            val labels = sel.map { if (it.startsWith("UD:")) "Custom: ${it.removePrefix("UD:")}" else it }
            ReviewRow(sq.label, if (labels.isEmpty()) "—" else labels.joinToString(", "))
        }
    }

    ReviewCard("Components") {
        Questions.ALL.filterIsInstance<WizardQuestion.CategoryQ>().forEach { cq ->
            val selIds = ui.state.catSel[cq.catKey].orEmpty()
            val entries = ui.catalog?.entries?.filter { it.category == cq.name && it.id in selIds }.orEmpty()
            ReviewRow(
                cq.name + if (cq.multi) " (multi)" else " (single)",
                if (entries.isEmpty()) "—" else entries.joinToString(", ") { it.name }
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    Button(
        onClick = {
            val text = vm.audit()?.auditPrompt() ?: return@Button
            copyToClipboard(ctx, text, "Audit prompt")
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary
        )
    ) {
        Text("Copy audit prompt")
    }
}
