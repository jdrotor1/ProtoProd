package com.example.projectscaffold.ui.wizard

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
                actions = { TextButton(onClick = { vm.reset() }) { Text("Reset") } }
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
