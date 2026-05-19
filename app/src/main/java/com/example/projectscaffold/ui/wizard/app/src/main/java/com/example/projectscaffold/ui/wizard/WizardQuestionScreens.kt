package com.example.projectscaffold.ui.wizard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.projectscaffold.model.CatalogEntry
import com.example.projectscaffold.model.WizardQuestion

@Composable
fun TextQuestionContent(q: WizardQuestion.Text, vm: WizardViewModel) {
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
fun SpecQuestionContent(q: WizardQuestion.Spec, vm: WizardViewModel) {
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
                    Checkbox(checked = selected, onCheckedChange = { vm.toggleSpec(q.key, opt, true, udText) })
                } else {
                    RadioButton(selected = selected, onClick = { vm.toggleSpec(q.key, opt, false, udText) })
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
fun CategoryQuestionContent(q: WizardQuestion.CategoryQ, vm: WizardViewModel) {
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
                "No entries in this category. Tap Next to skip — you can ask Claude in brainstorm or add manually later.",
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
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
                if (q.multi) {
                    Checkbox(checked = selected, onCheckedChange = { vm.toggleCategory(q.catKey, entry.id, true) })
                } else {
                    RadioButton(selected = selected, onClick = { vm.toggleCategory(q.catKey, entry.id, false) })
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
