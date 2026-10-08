package br.com.meushape.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.meushape.data.Repo
import br.com.meushape.logic.AtividadeFolga
import br.com.meushape.logic.Escala
import br.com.meushape.logic.TipoDia
import br.com.meushape.notify.Alarmes
import br.com.meushape.ui.theme.Cinza
import br.com.meushape.ui.theme.Limao
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Calendário da escala 12x36. Tocar num dia permite trocar o tipo manualmente. */
@Composable
fun CalendarioScreen(onVoltar: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { Repo.get(ctx) }
    val scope = rememberCoroutineScope()
    val escala by remember { repo.observarEscala() }.collectAsState(initial = Escala())
    var mes by remember { mutableStateOf(YearMonth.now()) }
    var editando by remember { mutableStateOf<LocalDate?>(null) }
    val hoje = Escala.diaLogico(LocalDateTime.now())

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onVoltar) { Text("← Voltar") }
        CartaoModoEscala(
            escala,
            onSalvar = { sem, ref -> scope.launch { repo.configurarEscala(sem, ref); Alarmes.agendarProximo(ctx) } },
            onHorario = { ini, fim -> scope.launch { repo.definirHorarioPlantao(ini, fim) } },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { mes = mes.minusMonths(1) }) { Text("‹") }
            Text(
                mes.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("pt", "BR"))).replaceFirstChar { it.uppercase() },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            OutlinedButton(onClick = { mes = mes.plusMonths(1) }) { Text("›") }
        }
        // Cabeçalho dos dias da semana (começa na segunda).
        Row {
            listOf("S", "T", "Q", "Q", "S", "S", "D").forEach {
                Text(it, Modifier.weight(1f), color = Cinza, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        val primeiro = mes.atDay(1)
        val vazios = primeiro.dayOfWeek.value - 1
        val totalCelulas = vazios + mes.lengthOfMonth()
        val semanas = (totalCelulas + 6) / 7
        for (s in 0 until semanas) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (d in 0 until 7) {
                    val indice = s * 7 + d - vazios
                    if (indice < 0 || indice >= mes.lengthOfMonth()) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val data = mes.atDay(indice + 1)
                        val info = escala.info(data)
                        val cor = corDoTipo(info.tipo)
                        Box(
                            Modifier.weight(1f).aspectRatio(1f)
                                .background(cor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .then(if (data == hoje) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp)) else Modifier)
                                .clickable { editando = data },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${data.dayOfMonth}", fontWeight = FontWeight.Bold, color = cor)
                                Text(
                                    when {
                                        info.tipo == TipoDia.PLANTAO -> "P"
                                        info.atividade == AtividadeFolga.CAMINHADA -> "🚶"
                                        else -> "🏋"
                                    },
                                    fontSize = 11.sp,
                                )
                            }
                            if (info.trocado) {
                                Box(Modifier.align(Alignment.TopEnd).padding(3.dp).size(6.dp).background(Color.White, CircleShape))
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Legenda", fontWeight = FontWeight.SemiBold)
        TipoDia.entries.forEach { t ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp).background(corDoTipo(t), RoundedCornerShape(3.dp)))
                Spacer(Modifier.width(8.dp))
                Text(t.nome)
            }
        }
        Text("🏋 treino de manhã · 🚶 caminhada · ponto branco = dia trocado manualmente", color = Cinza)
    }

    editando?.let { data ->
        val info = escala.info(data)
        var tipo by remember(data) { mutableStateOf<TipoDia?>(if (info.trocado) info.tipo else null) }
        var atividade by remember(data) { mutableStateOf<AtividadeFolga?>(null) }
        AlertDialog(
            onDismissRequest = { editando = null },
            title = { Text(data.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale("pt", "BR")))) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Automático: ${escala.tipoAutomatico(data).nome}", color = Cinza)
                    Text("Tipo do dia", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(selected = tipo == null, onClick = { tipo = null }, label = { Text("Auto") })
                        FilterChip(selected = tipo == TipoDia.PLANTAO, onClick = { tipo = TipoDia.PLANTAO }, label = { Text("Plantão") })
                        FilterChip(selected = tipo == TipoDia.FOLGA, onClick = { tipo = TipoDia.FOLGA }, label = { Text("Folga") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(selected = tipo == TipoDia.FERIAS, onClick = { tipo = TipoDia.FERIAS }, label = { Text("Férias") })
                        FilterChip(selected = tipo == TipoDia.FERIADO, onClick = { tipo = TipoDia.FERIADO }, label = { Text("Feriado") })
                    }
                    val ehFolga = (tipo ?: escala.tipoAutomatico(data)).usaCardapioFolga
                    if (ehFolga) {
                        Text("Manhã da folga", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(selected = atividade == null, onClick = { atividade = null }, label = { Text("Auto") })
                            FilterChip(selected = atividade == AtividadeFolga.TREINO, onClick = { atividade = AtividadeFolga.TREINO }, label = { Text("Treino") })
                            FilterChip(selected = atividade == AtividadeFolga.CAMINHADA, onClick = { atividade = AtividadeFolga.CAMINHADA }, label = { Text("Caminhada") })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val t = tipo
                    val a = if ((t ?: escala.tipoAutomatico(data)).usaCardapioFolga) atividade else null
                    scope.launch {
                        repo.trocarDia(data, t, a)
                        Alarmes.agendarProximo(ctx)
                    }
                    editando = null
                }) { Text("Salvar", color = Limao) }
            },
            dismissButton = { TextButton(onClick = { editando = null }) { Text("Cancelar") } },
        )
    }
}

/** Escolhe entre escala 12x36 e "sem escala", e o dia de plantão de referência. */
@Composable
private fun CartaoModoEscala(
    escala: Escala,
    onSalvar: (Boolean, LocalDate) -> Unit,
    onHorario: (java.time.LocalTime, java.time.LocalTime) -> Unit,
) {
    var escolhendoData by remember { mutableStateOf(false) }
    var escolhendoHora by remember { mutableStateOf<String?>(null) }
    br.com.meushape.ui.CartaoApp {
        Text("Minha escala", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = !escala.semEscala,
                onClick = { if (escala.semEscala) escolhendoData = true },
                label = { Text("Plantão 12x36") },
            )
            FilterChip(
                selected = escala.semEscala,
                onClick = { if (!escala.semEscala) onSalvar(true, escala.referenciaPlantao) },
                label = { Text("Sem escala") },
            )
        }
        if (escala.semEscala) {
            Text(
                "Todos os dias seguem a rotina de folga (treino e caminhada alternam). " +
                    "Ajuste os horários em Editar rotina → Folga.",
                color = Cinza,
            )
        } else {
            Text("Plantão de referência: ${escala.referenciaPlantao.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}", color = Cinza)
            TextButton(onClick = { escolhendoData = true }) { Text("Mudar o dia do plantão") }
            Text("Horário do plantão: ${escala.horarioPlantaoTexto}", color = Cinza)
            Row {
                TextButton(onClick = { escolhendoHora = "ini" }) { Text("Mudar início") }
                TextButton(onClick = { escolhendoHora = "fim" }) { Text("Mudar fim") }
            }
        }
    }
    escolhendoHora?.let { qual ->
        val atual = if (qual == "ini") escala.inicioPlantao else escala.fimPlantao
        br.com.meushape.ui.EscolherHora(atual.hour, atual.minute, onDismiss = { escolhendoHora = null }) { h, m ->
            val t = java.time.LocalTime.of(h, m)
            if (qual == "ini") onHorario(t, escala.fimPlantao) else onHorario(escala.inicioPlantao, t)
            escolhendoHora = null
        }
    }
    if (escolhendoData) {
        EscolherDataDialog(
            titulo = "Escolha um dia de PLANTÃO da escala",
            inicial = if (escala.semEscala) LocalDate.now() else escala.referenciaPlantao,
            onDismiss = { escolhendoData = false },
            onOk = { escolhendoData = false; onSalvar(false, it) },
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun EscolherDataDialog(titulo: String, inicial: LocalDate, onDismiss: () -> Unit, onOk: (LocalDate) -> Unit) {
    val utc = inicial.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    val estado = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = utc)
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onOk(java.time.Instant.ofEpochMilli(estado.selectedDateMillis ?: utc).atZone(java.time.ZoneOffset.UTC).toLocalDate())
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    ) {
        androidx.compose.material3.DatePicker(state = estado, title = { Text(titulo, Modifier.padding(16.dp)) })
    }
}
