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
                        val text = vm.prompts()?.brainstormPrompt(idx) ?: return@PerStepButtons
                        copyToClipboard(ctx, text, "Brainstorm prompt — Q${idx + 1}")
                    },
                    onBuild = {
                        val text = vm.prompts()?.buildPrompt(idx) ?: return@PerStepButtons
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

    Spacer(Modi
