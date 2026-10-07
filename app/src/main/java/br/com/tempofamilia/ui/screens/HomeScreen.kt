package br.com.tempofamilia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.tempofamilia.data.Store
import br.com.tempofamilia.ui.InfoCard
import br.com.tempofamilia.ui.rememberResumeTick
import br.com.tempofamilia.ui.theme.Laranja
import br.com.tempofamilia.ui.theme.Vermelho
import br.com.tempofamilia.util.Perms
import br.com.tempofamilia.util.Schedules
import br.com.tempofamilia.util.UsageTracker
import br.com.tempofamilia.util.formatMinutos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Resumo do dia: tempo usado e restante de cada app e o próximo horário da família. */
@Composable
fun HomeScreen(irParaApps: () -> Unit, irParaAjustes: () -> Unit) {
    val ctx = LocalContext.current
    val estado by Store.state.collectAsState()
    val tick = rememberResumeTick()
    var uso by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }

    // Atualiza o uso a cada 20 segundos enquanto a tela está aberta.
    LaunchedEffect(tick) {
        while (true) {
            uso = withContext(Dispatchers.IO) { runCatching { UsageTracker.usageToday(ctx) }.getOrDefault(emptyMap()) }
            delay(20_000)
        }
    }
    val faltando = remember(tick) {
        buildList {
            if (!Perms.usage(ctx)) add("Acesso ao uso")
            if (!Perms.overlay(ctx)) add("Sobreposição")
            if (!Perms.accessibility(ctx)) add("Acessibilidade")
            if (!Perms.admin(ctx)) add("Administrador do dispositivo")
        }
    }
    val hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "BR")))
    val ativo = Schedules.activeNow(estado.schedules)
    val proximo = Schedules.next(estado.schedules)

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(top = 16.dp)) {
                Text("Tempo Família", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(hoje.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (estado.weeklyReleaseEnabled) {
            item { CartaoLiberacaoSemanal(estado) }
        }
        if (faltando.isNotEmpty()) {
            item {
                InfoCard(
                    "Proteção incompleta",
                    "Falta ativar: ${faltando.joinToString(", ")}. Veja em Ajustes → Permissões.",
                    Vermelho.copy(alpha = 0.15f),
                ) { Button(onClick = irParaAjustes) { Text("Resolver") } }
            }
        }
        item {
            when {
                ativo != null -> InfoCard(
                    "Agora: ${ativo.name}",
                    "Horário da família até ${Schedules.hora(ativo.endMin)}. Os apps limitados estão bloqueados.",
                    Laranja.copy(alpha = 0.25f),
                )
                proximo != null -> InfoCard(
                    "Próximo horário da família",
                    "${proximo.first.name} — ${Schedules.quando(proximo.second)} até ${Schedules.hora(proximo.first.endMin)}",
                    MaterialTheme.colorScheme.secondaryContainer,
                )
                else -> InfoCard(
                    "Horários da família",
                    "Nenhum horário cadastrado. Crie um na aba Horários (ex.: jantar, hora das crianças).",
                    MaterialTheme.colorScheme.secondaryContainer,
                )
            }
        }
        item { Text("Uso de hoje", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        if (estado.limits.isEmpty()) {
            item {
                InfoCard("Nenhum app limitado", "Adicione Instagram, TikTok, YouTube e outros.", MaterialTheme.colorScheme.primaryContainer) {
                    Button(onClick = irParaApps) { Text("Adicionar apps") }
                }
            }
        }
        items(estado.limits, key = { it.packageName }) { lim ->
            val usadoMin = (uso[lim.packageName] ?: 0L) / 60_000L
            val restante = lim.minutesPerDay - usadoMin
            val fracao = if (lim.minutesPerDay == 0) 1f else (usadoMin.toFloat() / lim.minutesPerDay).coerceIn(0f, 1f)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(lim.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (restante > 0) "Restam ${formatMinutos(restante)}" else "Limite atingido",
                            color = if (restante > 0) MaterialTheme.colorScheme.primary else Vermelho,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { fracao },
                        color = if (fracao >= 1f) Vermelho else if (fracao > 0.8f) Laranja else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Usado ${formatMinutos(usadoMin)} de ${formatMinutos(lim.minutesPerDay.toLong())}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        item { Text(" ") }
    }
}

/** Cartão da liberação semanal: usar, acompanhar o tempo e encerrar antes. */
@Composable
private fun CartaoLiberacaoSemanal(estado: br.com.tempofamilia.data.AppState) {
    var agora by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(estado.weeklyReleaseStartedAt) {
        while (true) { agora = System.currentTimeMillis(); delay(1000) }
    }
    var confirmando by remember { mutableStateOf(false) }
    val emAndamento = br.com.tempofamilia.util.LiberacaoSemanal.emAndamento(estado, agora)
    val disponivel = br.com.tempofamilia.util.LiberacaoSemanal.disponivel(estado)
    val semanas = br.com.tempofamilia.util.LiberacaoSemanal.semanasSemUsar(estado)

    when {
        emAndamento -> {
            val resta = ((br.com.tempofamilia.util.LiberacaoSemanal.terminaEm(estado) - agora) / 1000).coerceAtLeast(0)
            InfoCard(
                "Liberação semanal em andamento",
                "O bloqueio de sites volta sozinho em %d:%02d.".format(resta / 60, resta % 60),
                Laranja.copy(alpha = 0.25f),
            ) {
                Button(onClick = {
                    Store.update { br.com.tempofamilia.util.LiberacaoSemanal.encerrar(it) }
                }) { Text("Encerrar agora e voltar a bloquear") }
            }
        }
        disponivel -> InfoCard(
            "Liberação semanal disponível",
            "15 minutos, ${if (estado.weeklyReleaseIntervalWeeks <= 1) "uma vez por semana" else "uma vez a cada ${estado.weeklyReleaseIntervalWeeks} semanas"}. Cada semana sem usar é uma vitória." +
                if (semanas > 0) "\n🏆 $semanas ${if (semanas == 1) "semana" else "semanas seguidas"} sem usar." else "",
            MaterialTheme.colorScheme.secondaryContainer,
        ) {
            OutlinedButton(onClick = { confirmando = true }) { Text("Usar os 15 minutos desta semana") }
        }
        else -> InfoCard(
            "Liberação já usada",
            "Tudo bloqueado até segunda, ${br.com.tempofamilia.util.LiberacaoSemanal.proximaSegunda(estado).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))}." +
                if (semanas > 0) "\n🏆 $semanas ${if (semanas == 1) "semana" else "semanas seguidas"} sem usar antes desta." else "",
            MaterialTheme.colorScheme.primaryContainer,
        )
    }

    if (confirmando) {
        AlertDialog(
            onDismissRequest = { confirmando = false },
            title = { Text("Tem certeza?") },
            text = {
                Text(
                    "Depois de usar, a próxima liberação só vem daqui a ${estado.weeklyReleaseIntervalWeeks.coerceAtLeast(1)} semana(s), e o tempo conta mesmo se fechar antes.\n\n" +
                        "Se a vontade passar, é só cancelar: mais uma semana sem usar conta para a sua meta."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmando = false
                    Store.update { br.com.tempofamilia.util.LiberacaoSemanal.iniciar(it) }
                }) { Text("Usar agora") }
            },
            dismissButton = { TextButton(onClick = { confirmando = false }) { Text("Cancelar, não vou usar") } },
        )
    }
}
