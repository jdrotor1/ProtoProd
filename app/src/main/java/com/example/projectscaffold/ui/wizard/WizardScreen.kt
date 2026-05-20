package com.example.projectscaffold.ui.wizard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.default.ArrowBack
import androidx.compose.material.icons.default.Check
import androidx.compose.materiql.icons.default.ContentCopy
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.projectscaffold.ui.wizard.WizardViewModel
import com.example.projectscaffold.model.CatalogEntry
import com.example.projectscaffold.model.Questions
import com.example.projectscaffold.model.WizardQuestion

@OptIn(ExperimentalMaterial3Api::class)
// Move this function to top-level (outside of composables)
internal fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied — paste in Claude", Toast.LENGTH_SHORT).show()
}
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
            onDismissRequest = { vm.resumeOrDiscard(false) },
            confirmButton = { TextButton(onClick = { vm.resumeOrDiscard(true) }) { Text("Resume") } },
            dismissButton = { TextButton(onClick = { vm.resumeOrDiscard(false) }) { Text("Discard") } },
            title = { Text("Unfinished project") },
            text = { Text("You have a wizard in progress. Resume where you left off, or discard and start fresh?") }
        )
        return
    }

    val idx = ui.state.idx
    val total = Questions.TOTAL
    
    // Bounds check to prevent IndexOutOfBoundsException
    if (idx < 0 || idx >= Questions.ALL.size) {
        Text("Error: Invalid question index")
        return
    }
    
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        enabled = idx > 0,
                        onClick = { vm.back() },
                        modifier = Modifier.weight(0.4f)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Back")
                    }
                    Spacer(Modifier.weight(0.2f))
                    if (q is WizardQuestion.Review) {
                        Text(
                            "End of wizard",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(0.4f)
                        )
                    } else {
                        Button(
                            onClick = { vm.next() },
                            modifier = Modifier.weight(0.4f)
                        ) {
                            Text("Next")
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Next",
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            AnimatedContent(
                targetState = q,
                label = "Question transition",
                transitionSpec = {
                    slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                            slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                }
            ) { question ->
                when (question) {
                    is WizardQuestion.Text -> TextQuestionContent(question, vm)
                    is WizardQuestion.Spec -> SpecQuestionContent(question, vm)
                    is WizardQuestion.CategoryQ -> CategoryQuestionContent(question, vm)
                    is WizardQuestion.Review -> ReviewContent(vm)
                }
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
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onBrainstorm,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy",
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Brainstorm")
        }
        OutlinedButton(
            onClick = onBuild,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy",
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Build")
        }
    }
}

@Composable
private fun TextQuestionContent(q: WizardQuestion.Text, vm: WizardViewModel) {
    val ui by vm.ui.collectAsState()
    Spacer(Modifier.height(8.dp))
    Text(
        q.label + if (q.required) " *" else "",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
    if (q.help.isNotBlank()) {
        Text(
            q.help,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(top = 8.dp)
                .alpha(0.7f)
        )
    }
    OutlinedTextField(
        value = ui.state.texts[q.key].orEmpty(),
        onValueChange = { vm.updateText(q.key, it) },
        placeholder = { Text(q.help) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        singleLine = false,
        minLines = 3
    )
}

@Composable
private fun SpecQuestionContent(q: WizardQuestion.Spec, vm: WizardViewModel) {
    val ui by vm.ui.collectAsState()
    val sel = ui.state.specSel[q.key].orEmpty()
    val udText = ui.state.udText[q.key].orEmpty()

    Spacer(Modifier.height(8.dp))
    Text(
        q.label,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
    Text(
        if (q.multi) "Choose one or more. [User Defined] reveals a custom field."
        else "Choose one. [User Defined] reveals a custom field.",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .padding(top = 8.dp, bottom = 12.dp)
            .alpha(0.7f)
    )

    q.options.forEach { opt ->
        val isUD = opt == "[User Defined]"
        val selected = if (isUD) sel.any { it.startsWith("UD:") } else opt in sel
        
        AnimatedContent(
            targetState = selected,
            label = "Selection state"
        ) { isSelected ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
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
                    Spacer(Modifier.width(8.dp))
                    Text(
                        opt,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = sel.any { it.startsWith("UD:") },
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        OutlinedTextField(
            value = udText,
            onValueChange = { vm.updateUdText(q.key, it) },
            label = { Text("Custom value") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
    }
}

@Composable
private fun CategoryQuestionContent(q: WizardQuestion.CategoryQ, vm: WizardViewModel) {
    val ui by vm.ui.collectAsState()
    val sel = ui.state.catSel[q.catKey].orEmpty()
    val entries = ui.catalog?.entries?.filter { it.category == q.name }.orEmpty()

    Spacer(Modifier.height(8.dp))
    Text(
        q.name,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
    Text(
        (if (q.multi) "Choose one or more components." else "Choose one component.")
            + if (q.parts) " From the embedded catalog." else "",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .padding(top = 8.dp, bottom = 12.dp)
            .alpha(0.7f)
    )

    if (entries.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
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
        AnimatedContent(
            targetState = selected,
            label = "Category selection"
        ) { isSelected ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            entry.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        when (entry) {
                            is CatalogEntry.Parts -> {
                                val bits = mutableListOf<String>()
                                if (entry.data.manufacturer.isNotBlank()) bits.add(entry.data.manufacturer)
                                if (entry.data.mpn.isNotBlank()) bits.add(entry.data.mpn)
                                if (entry.data.unit_cost_usd > 0) bits.add("$${entry.data.unit_cost_usd}")
                                if (bits.isNotEmpty()) {
                                    Text(
                                        bits.joinToString("  ·  "),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .alpha(0.8f)
                                    )
                                }
                                if (entry.data.lifecycle_status != "active") {
                                    Text(
                                        entry.data.lifecycle_status,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                            is CatalogEntry.NonParts -> {
                                if (entry.data.notes.isNotBlank()) {
                                    Text(
                                        entry.data.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .alpha(0.8f)
                                    )
                                }
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
    Text(
        "Build Summary",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
    Text(
        "All 33 step prompts have been collected. By the end of Q33's build prompt, Claude has produced and assembled the 17 scaffold files. Use the audit button below to verify the assembled set.",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .padding(top = 8.dp, bottom = 16.dp)
            .alpha(0.8f)
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
        Icon(
            Icons.Filled.ContentCopy,
            contentDescription = "Copy",
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text("Copy audit prompt")
    }
}

@Composable
private fun ReviewCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ReviewRow(key: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            key,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
