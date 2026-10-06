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
