package br.com.meushape.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import br.com.meushape.data.Config
import br.com.meushape.data.Repo
import br.com.meushape.data.RegistroSono
import br.com.meushape.data.StatusSono
import br.com.meushape.data.ldt
import br.com.meushape.data.ms
import br.com.meushape.logic.Periodo
import br.com.meushape.logic.SonoCalc
import br.com.meushape.service.MonitorService
import br.com.meushape.ui.CartaoApp
import br.com.meushape.ui.EscolherDataHora
import br.com.meushape.ui.theme.Azul
import br.com.meushape.ui.theme.Cinza
import br.com.meushape.ui.theme.Laranja
import br.com.meushape.ui.theme.Limao
import br.com.meushape.ui.theme.Vermelho
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** "7h 25min". */
fun horas(min: Long): String = "%dh %02dmin".format(min / 60, min % 60)

private val FMT_HORA = DateTimeFormatter.ofPattern("HH:mm")
private val FMT_DIA_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm")

/** Texto "das 23:10 às 06:30". */
fun faixa(ini: LocalDateTime, fim: LocalDateTime) = "das ${ini.format(FMT_HORA)} às ${fim.format(FMT_HORA)}"

@SuppressLint("BatteryLife")
@Composable
fun SonoAba() {
    val ctx = LocalContext.current
    val repo = remember { Repo.get(ctx) }
    val scope = rememberCoroutineScope()
    var agora by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(60_000); agora = LocalDateTime.now() } }
    val desde = remember { System.currentTimeMillis() - 14L * 24 * 3600 * 1000 }
    val registros by remember { repo.db.sono().observarDesde(desde) }.collectAsState(initial = emptyList())
    val limite by remember {
        repo.db.config().observar().map { l -> l.firstOrNull { it.chave == "sono_limite_min" }?.valor?.toIntOrNull() ?: 25 }
    }.collectAsState(initial = 25)
    var editando by remember { mutableStateOf<RegistroSono?>(null) }
    var verificador by remember { mutableIntStateOf(0) }

    val confirmados = registros.filter { it.status == StatusSono.CONFIRMADO }.map { Periodo(it.inicio.ldt(), it.fim.ldt()) }
    val total24 = SonoCalc.ultimas24h(confirmados, agora)
    val media = SonoCalc.mediaSemana(confirmados, agora)

    // Permissões e bateria
    val pm = ctx.getSystemService(PowerManager::class.java)
    val semOtimizacao = remember(verificador) { pm.isIgnoringBatteryOptimizations(ctx.packageName) }
    val permPassos = remember(verificador) {
        Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACTIVITY_RECOGNITION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    val pedirPassos = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        verificador++
        MonitorService.iniciar(ctx, MonitorService.ACAO_PASSOS)
    }
    var explicandoBateria by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("Sono", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        }
        item {
            CartaoApp(cor = if (total24 < SonoCalc.META_MIN) Laranja.copy(alpha = 0.2f) else br.com.meushape.ui.theme.Cartao) {
                Text("Últimas 24 horas", color = Cinza)
                Text(horas(total24), fontSize = 36.sp, fontWeight = FontWeight.Bold,
                    color = if (total24 < SonoCalc.META_MIN) Laranja else Limao)
                LinearProgressIndicator(
                    progress = { (total24.toFloat() / SonoCalc.META_MIN).coerceAtMost(1f) },
                    color = if (total24 < SonoCalc.META_MIN) Laranja else Limao,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
                Spacer(Modifier.height(6.dp))
                if (total24 < SonoCalc.META_MIN) Text("⚠️ Abaixo de 7 horas. Tente encaixar um cochilo.", color = Laranja)
                Text("Média dos últimos 7 dias: ${horas(media)} por dia", color = if (media < SonoCalc.META_MIN) Laranja else Cinza)
                Text("Soma sono + cochilos confirmados.", color = Cinza, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (!semOtimizacao) {
            item {
                CartaoApp(cor = Vermelho.copy(alpha = 0.18f)) {
                    Text("Liberar funcionamento em segundo plano", fontWeight = FontWeight.Bold)
                    Text("Sem isso o Android pode desligar o monitor durante a noite e o sono não é detectado.")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { explicandoBateria = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Liberar") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val fim = LocalDateTime.now().withSecond(0).withNano(0)
                        editando = RegistroSono(inicio = fim.minusHours(1).ms(), fim = fim.ms(), status = StatusSono.CONFIRMADO, manual = true)
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                ) { Text("+ Adicionar sono") }
            }
        }
        item { Text("Registros", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) }
        if (registros.isEmpty()) item {
            Text(
                "Nenhum registro ainda. Quando o celular ficar mais de $limite minutos sem uso, " +
                    "o app pergunta se você estava dormindo.",
                color = Cinza,
            )
        }
        items(registros.filter { it.status != StatusSono.RECUSADO }, key = { it.id }) { r ->
            val ini = r.inicio.ldt(); val fim = r.fim.ldt()
            CartaoApp(Modifier.clickable { editando = r }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${ini.format(FMT_DIA_HORA)} → ${fim.format(FMT_HORA)}", fontWeight = FontWeight.SemiBold)
                        Text(
                            horas(Periodo(ini, fim).minutos) + when {
                                r.status == StatusSono.PENDENTE -> " · aguardando confirmação"
                                r.manual -> " · manual"
                                else -> ""
                            },
                            color = if (r.status == StatusSono.PENDENTE) Laranja else Cinza,
                        )
                    }
                    Text("✎", color = Cinza)
                }
            }
        }
        item {
            CartaoApp {
                Text("Detecção automática", fontWeight = FontWeight.SemiBold)
                Text("Considerar sono após quantos minutos sem usar o telefone:", color = Cinza)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        scope.launch { repo.db.config().salvar(Config("sono_limite_min", (limite - 5).coerceAtLeast(10).toString())) }
                    }, modifier = Modifier.size(52.dp)) { Text("−") }
                    Text("$limite min", Modifier.width(100.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    OutlinedButton(onClick = {
                        scope.launch { repo.db.config().salvar(Config("sono_limite_min", (limite + 5).coerceAtMost(120).toString())) }
                    }, modifier = Modifier.size(52.dp)) { Text("+") }
                }
                Text(
                    "Plantão (17:40–05:40) e horários de treino nunca contam como sono.",
                    color = Cinza, style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            CartaoApp {
                Text("👟 Contador de passos", fontWeight = FontWeight.SemiBold)
                when {
                    !MonitorService.temSensorDePassos(ctx) -> Text("Este celular não tem sensor de passos. O contador fica desativado.", color = Cinza)
                    !permPassos -> {
                        Text("Precisa da permissão \"Atividade física\" para contar os passos.", color = Cinza)
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = { if (Build.VERSION.SDK_INT >= 29) pedirPassos.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) { Text("Permitir contar passos") }
                    }
                    else -> Text("Ativo. Meta: 8.000 passos por dia (veja na tela Hoje).", color = Limao)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (explicandoBateria) {
        AlertDialog(
            onDismissRequest = { explicandoBateria = false },
            title = { Text("Por que liberar?") },
            text = {
                Text(
                    "O Meu Shape precisa ficar ligado em segundo plano para perceber quando você para de usar " +
                        "o telefone (sono) e contar os passos. Para economizar bateria, o Android costuma desligar " +
                        "apps em segundo plano — por isso pedimos para ignorar a otimização de bateria.\n\n" +
                        "No Realme, também vale: Configurações → Apps → Meu Shape → Uso da bateria → " +
                        "\"Permitir atividade em segundo plano\" e \"Permitir inicialização automática\"."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    explicandoBateria = false
                    try {
                        ctx.startActivity(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
                        )
                    } catch (_: Exception) {
                    }
                    verificador++
                }) { Text("Continuar") }
            },
            dismissButton = { TextButton(onClick = { explicandoBateria = false }) { Text("Agora não") } },
        )
    }
    editando?.let { r ->
        EditarSonoDialog(
            r = r,
            onDismiss = { editando = null },
            onSalvar = { novo -> editando = null; scope.launch { repo.db.sono().salvar(novo) } },
            onApagar = if (r.id == 0L) null else {
                { editando = null; scope.launch { repo.db.sono().apagar(r) } }
            },
        )
    }
    // Revalida permissões ao voltar para a tela.
    LaunchedEffect(Unit) { while (true) { delay(3000); verificador++ } }
}

@Composable
fun EditarSonoDialog(
    r: RegistroSono,
    onDismiss: () -> Unit,
    onSalvar: (RegistroSono) -> Unit,
    onApagar: (() -> Unit)?,
    titulo: String = if (r.id == 0L) "Adicionar sono" else "Editar sono",
) {
    var ini by remember { mutableStateOf(r.inicio.ldt()) }
    var fim by remember { mutableStateOf(r.fim.ldt()) }
    var escolhendo by remember { mutableStateOf<String?>(null) }
    val valido = fim.isAfter(ini)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { escolhendo = "ini" }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Dormiu: ${ini.format(FMT_DIA_HORA)}")
                }
                OutlinedButton(onClick = { escolhendo = "fim" }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Acordou: ${fim.format(FMT_DIA_HORA)}")
                }
                Text(if (valido) "Total: ${horas(Periodo(ini, fim).minutos)}" else "O fim precisa ser depois do início",
                    color = if (valido) Limao else Vermelho)
                if (onApagar != null) TextButton(onClick = onApagar) { Text("Apagar registro", color = Vermelho) }
            }
        },
        confirmButton = {
            TextButton(enabled = valido, onClick = {
                onSalvar(r.copy(inicio = ini.ms(), fim = fim.ms(), status = StatusSono.CONFIRMADO))
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
    escolhendo?.let { qual ->
        EscolherDataHora(if (qual == "ini") ini else fim, onDismiss = { escolhendo = null }) {
            if (qual == "ini") ini = it else fim = it
            escolhendo = null
        }
    }
}

/** Pergunta feita ao abrir o app para cada sono detectado e ainda não confirmado. */
@Composable
fun PerguntaSono() {
    val ctx = LocalContext.current
    val repo = remember { Repo.get(ctx) }
    val scope = rememberCoroutineScope()
    val pendentes by remember { repo.db.sono().observarPendentes() }.collectAsState(initial = emptyList())
    var ajustando by remember { mutableStateOf<RegistroSono?>(null) }
    val r = pendentes.firstOrNull() ?: return

    if (ajustando != null) {
        EditarSonoDialog(
            r = ajustando!!, titulo = "Ajustar horários",
            onDismiss = { ajustando = null },
            onSalvar = { novo -> ajustando = null; scope.launch { repo.db.sono().salvar(novo) } },
            onApagar = null,
        )
        return
    }
    val ini = r.inicio.ldt(); val fim = r.fim.ldt()
    AlertDialog(
        onDismissRequest = {},
        title = { Text("😴 Sono detectado") },
        text = {
            Column {
                Text("Você estava dormindo ${faixa(ini, fim)}?", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("${ini.format(DateTimeFormatter.ofPattern("dd/MM"))} · ${horas(Periodo(ini, fim).minutos)}", color = Cinza)
                if (pendentes.size > 1) Text("Mais ${pendentes.size - 1} para confirmar", color = Azul)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { scope.launch { repo.db.sono().salvar(r.copy(status = StatusSono.CONFIRMADO)) } },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Sim") }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { scope.launch { repo.db.sono().salvar(r.copy(status = StatusSono.RECUSADO)) } },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Não (estava fazendo outra coisa)") }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { ajustando = r }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Ajustar horários")
                }
            }
        },
        confirmButton = {},
    )
}
