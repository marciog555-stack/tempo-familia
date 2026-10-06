package br.com.meushape.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.com.meushape.ui.theme.Cartao

/** Cartão padrão do app. */
@Composable
fun CartaoApp(
    modifier: Modifier = Modifier,
    cor: Color = Cartao,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cor),
    ) {
        Column(Modifier.padding(16.dp), content = conteudo)
    }
}

/** Relógio para escolher um horário (formato 24h). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EscolherHora(hora: Int, minuto: Int, onDismiss: () -> Unit, onOk: (Int, Int) -> Unit) {
    val estado = rememberTimePickerState(initialHour = hora, initialMinute = minuto, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = estado) },
        confirmButton = { TextButton(onClick = { onOk(estado.hour, estado.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Escolhe data e depois hora (em sequência). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EscolherDataHora(
    inicial: java.time.LocalDateTime,
    onDismiss: () -> Unit,
    onOk: (java.time.LocalDateTime) -> Unit,
) {
    var data by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<java.time.LocalDate?>(null) }
    if (data == null) {
        val utc = inicial.toLocalDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val estado = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = utc)
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    data = java.time.Instant.ofEpochMilli(estado.selectedDateMillis ?: utc)
                        .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                }) { Text("Próximo") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        ) { androidx.compose.material3.DatePicker(state = estado) }
    } else {
        EscolherHora(inicial.hour, inicial.minute, onDismiss) { h, m -> onOk(data!!.atTime(h, m)) }
    }
}
