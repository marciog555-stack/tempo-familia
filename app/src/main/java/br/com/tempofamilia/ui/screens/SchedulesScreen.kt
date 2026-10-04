package br.com.tempofamilia.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.tempofamilia.data.FamilySchedule
import br.com.tempofamilia.data.Store
import br.com.tempofamilia.ui.rememberPasswordGate
import br.com.tempofamilia.util.Schedules

/** Horários da família. Criar é livre; alterar ou remover pede a senha. */
@Composable
fun SchedulesScreen() {
    val estado by Store.state.collectAsState()
    val gate = rememberPasswordGate()
    var criando by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<FamilySchedule?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { criando = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Novo horário") },
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(Modifier.padding(top = 16.dp)) {
                    Text("Horários da família", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Nestes horários todos os apps limitados ficam totalmente bloqueados, " +
                            "mesmo que ainda tenham tempo sobrando.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (estado.schedules.isEmpty()) {
                item { Text("Nenhum horário ainda. Exemplos: Jantar 19:00–20:30, Hora das crianças 18:00–19:00.", modifier = Modifier.padding(vertical = 24.dp)) }
            }
            items(estado.schedules, key = { it.id }) { h ->
                val ativo = Schedules.isActive(h)
                Card(Modifier.fillMaxWidth().clickable {
                    gate.ask("Alterar o horário \"${h.name}\".") { editando = h }
                }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                h.name + if (ativo) "  • agora" else "",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                            )
                            Text("${Schedules.hora(h.startMin)} – ${Schedules.hora(h.endMin)}")
                            Text(Schedules.dias(h.days), style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = {
                            gate.ask("Remover o horário \"${h.name}\".") {
                                Store.update { s -> s.copy(schedules = s.schedules.filterNot { it.id == h.id }) }
                            }
                        }) { Icon(Icons.Filled.Delete, contentDescription = "Remover") }
                    }
                }
            }
            item { Spacer(Modifier.padding(40.dp)) }
        }
    }

    if (criando) {
        HorarioDialog(null, onDismiss = { criando = false }) { novo ->
            criando = false
            Store.update { s -> s.copy(schedules = s.schedules + novo) }
        }
    }
    editando?.let { h ->
        HorarioDialog(h, onDismiss = { editando = null }) { alterado ->
            editando = null
            Store.update { s -> s.copy(schedules = s.schedules.map { if (it.id == h.id) alterado else it }) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HorarioDialog(inicial: FamilySchedule?, onDismiss: () -> Unit, onSalvar: (FamilySchedule) -> Unit) {
    var nome by remember { mutableStateOf(inicial?.name ?: "Jantar em família") }
    var dias by remember { mutableStateOf(inicial?.days ?: setOf(1, 2, 3, 4, 5, 6, 7)) }
    var inicio by remember { mutableIntStateOf(inicial?.startMin ?: (19 * 60)) }
    var fim by remember { mutableIntStateOf(inicial?.endMin ?: (20 * 60 + 30)) }
    var escolhendoHora by remember { mutableStateOf<Int?>(null) } // 0 = início, 1 = fim

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (inicial == null) "Novo horário" else "Alterar horário") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nome, onValueChange = { nome = it.take(40) },
                    label = { Text("Nome") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Schedules.NOMES_DIAS.forEachIndexed { i, d ->
                        val dia = i + 1
                        FilterChip(
                            selected = dia in dias,
                            onClick = { dias = if (dia in dias) dias - dia else dias + dia },
                            label = { Text(d) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { escolhendoHora = 0 }, modifier = Modifier.weight(1f)) {
                        Text("Início ${Schedules.hora(inicio)}")
                    }
                    OutlinedButton(onClick = { escolhendoHora = 1 }, modifier = Modifier.weight(1f)) {
                        Text("Fim ${Schedules.hora(fim)}")
                    }
                }
                if (fim < inicio) Text("Atravessa a meia-noite.", style = MaterialTheme.typography.bodySmall)
                if (fim == inicio) Text("Início igual ao fim: bloqueia o dia inteiro.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSalvar(
                        FamilySchedule(
                            id = inicial?.id ?: System.currentTimeMillis(),
                            name = nome.trim().ifEmpty { "Horário da família" },
                            days = dias, startMin = inicio, endMin = fim,
                        )
                    )
                },
                enabled = dias.isNotEmpty(),
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )

    escolhendoHora?.let { qual ->
        val atual = if (qual == 0) inicio else fim
        EscolherHoraDialog(atual, onDismiss = { escolhendoHora = null }) { min ->
            if (qual == 0) inicio = min else fim = min
            escolhendoHora = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EscolherHoraDialog(atual: Int, onDismiss: () -> Unit, onOk: (Int) -> Unit) {
    val estado = rememberTimePickerState(initialHour = atual / 60, initialMinute = atual % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = estado) },
        confirmButton = { TextButton(onClick = { onOk(estado.hour * 60 + estado.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
