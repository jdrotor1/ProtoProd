package com.example.projectscaffold.ui.wizard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.projectscaffold.model.CatalogEntry
import com.example.projectscaffold.model.Questions
import com.example.projectscaffold.model.WizardQuestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardScreen(vm: WizardViewModel = viewModel()) {
    val ui by vm.ui.collectAsState()
    val ctx = LocalContext.current

    ui.transientError?.let { msg ->
        LaunchedEffect(msg) {
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            vm.consumeTransientError()
        }
    }

    if (ui.hasResumableState) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = { TextButton(onClick = { vm.resumeOrDiscard(true) }) { Text("Resume") } },
            dismissButton = { TextButton(onClick = { vm.resumeOrDiscard(false) }) { Text("Discard") } },
            title = { Text("Unfinished project") },
            text = { Text("You have a wizard in progress. Resume where you left off, or discard and start fresh?") }
        )
        return
    }

    val idx = ui.state.idx
    val total = Questions.TOTAL
    val q = Questions.ALL[idx]

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Project Scaffold", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (q is WizardQuestion.Review) "Build Summary (Question ${idx + 1} of $total)"
                            else "Question ${idx + 1} of $total",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { vm.reset() }) { Text("Reset") }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(enabled = idx > 0, onClick = { vm.back() }) { Text("Back") }
                    Spacer(Modifier.weight(1f))
                    if (q is WizardQuestion.Review) {
                        Text("End of wizard", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Button(onClick = { vm.next() }) { Text("Next") }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            LinearProgressIndicator(
                progress = { (idx.toFloat() / (total - 1).toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            Text("Question ${idx + 1} of $total", style = MaterialTheme.typography.labelMedium)

            when (q) {
                is WizardQuestion.Text       -> TextQuestionContent(q, vm)
                is WizardQuestion.Spec       -> SpecQuestionContent(q, vm)
                is WizardQuestion.CategoryQ  -> CategoryQuestionContent(q, vm)
                is WizardQuestion.Review     -> ReviewContent(vm)
            }

            if (q !is WizardQuestion.Review) {
                Spacer(Modifier.height(16.dp))
                PerStepButtons(
                    onBrainstorm = {
                        val text = vm.brainstorm()?.brainstormPrompt(idx) ?: return@PerStepButtons
                        copyToClipboard(ctx, text, "Brainstorm prompt — Q${idx + 1}")
                    },
                    onBuild = {
                        val text = vm.brainstorm()?.buildPrompt(idx) ?: return@PerStepButtons
                        copyToClipboard(ctx, text, "Build prompt — Q${idx + 1}")
                    }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PerStepButtons(onBrainstorm: () -> Unit, onBuild: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onBrainstorm, modifier = Modifier.weight(1f)) { Text("Copy brainstorm") }
        OutlinedButton(onClick = onBuild, modifier = Modifier.weight(1f)) { Text("Copy build") }
    }
}

@Composable
private fun TextQuestionContent(q: WizardQuestion.Text, vm: WizardViewModel) {
    val ui by vm.ui.collectAsState()
    Spacer(Modifier.height(8.dp))
    Text(q.label + if (q.required) " *" else "", style = MaterialTheme.typography.titleLarge)
    if (q.help.isNotBlank()) {
        Text(q.help, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
    }
    OutlinedTextField(
        value = ui.state.texts[q.key].orEmpty(),
        onValueChange = { vm.updateText(q.key, it) },
        placeholder = { Text(q.help) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
}

@Composable
private fun SpecQuestionContent(q: WizardQuestion.Spec, vm: WizardViewModel) {
    val ui by vm.ui.collectAsState()
    val sel = ui.state.specSel[q.key].orEmpty()
    val udText = ui.state.udText[q.key].orEmpty()

    Spacer(Modifier.height(8.dp))
    Text(q.label, style = MaterialTheme.typography.titleLarge)
    Text(
        if (q.multi) "Choose one or more. [User Defined] reveals a custom field."
        else "Choose one. [User Defined] reveals a custom field.",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
    )

    q.options.forEach { opt ->
        val isUD = opt == "[User Defined]"
        val selected = if (isUD) sel.any { it.startsWith("UD:") } else opt in sel
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                  else MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (q.multi) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { vm.toggleSpec(q.key, opt, true, udText) }
                    )
                } else {
                    RadioButton(
                        selected = selected,
                        onClick = { vm.toggleSpec(q.key, opt, false, udText) }
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(opt, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (sel.any { it.startsWith("UD:") }) {
        OutlinedTextField(
            value = udText,
            onValueChange = { vm.updateUdText(q.key, it) },
            label = { Text("Custom value") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}

@Composable
private fun CategoryQuestionContent(q: WizardQuestion.CategoryQ, vm: WizardViewModel) {
    val ui by vm.ui.collectAsState()
    val sel = ui.state.catSel[q.catKey].orEmpty()
    val entries = ui.catalog?.entries?.filter { it.category == q.name }.orEmpty()

    Spacer(Modifier.height(8.dp))
    Text(q.name, style = MaterialTheme.typography.titleLarge)
    Text(
        (if (q.multi) "Choose one or more components." else "Choose one component.")
            + if (q.parts) " From the embedded catalog." else "",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
    )

    if (entries.isEmpty()) {
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(
                "No entries in this category in the embedded catalog. Tap Next to skip — you can ask Claude in brainstorm or add manually later.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    entries.forEach { entry ->
        val selected = entry.id in sel
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                  else MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (q.multi) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { vm.toggleCategory(q.catKey, entry.id, true) }
                    )
                } else {
                    RadioButton(
                        selected = selected,
                        onClick = { vm.toggleCategory(q.catKey, entry.id, false) }
                    )
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    when (entry) {
                        is CatalogEntry.Parts -> {
                            val bits = mutableListOf<String>()
                            if (entry.data.manufacturer.isNotBlank()) bits.add(entry.data.manufacturer)
                            if (entry.data.mpn.isNotBlank()) bits.add(entry.data.mpn)
                            if (entry.data.unit_cost_usd > 0) bits.add("$${entry.data.unit_cost_usd}")
                            if (bits.isNotEmpty()) {
                                Text(bits.joinToString("  ·  "), style = MaterialTheme.typography.bodySmall)
                            }
                            if (entry.data.lifecycle_status != "active") {
                                Text(entry.data.lifecycle_status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        is CatalogEntry.NonParts -> {
                            if (entry.data.notes.isNotBlank()) {
                                Text(entry.data.notes, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewContent(vm: WizardViewModel) {
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
            val text = vm.prompts()?.auditPrompt() ?: return@Button
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

@Composable
private fun ReviewCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ReviewRow(key: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(key, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun copyToClipboard(ctx: Context, text: String, label: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(ctx, "$label copied — paste in Claude", Toast.LENGTH_SHORT).show()
}
