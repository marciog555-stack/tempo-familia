package br.com.meushape.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.meushape.data.Agua
import br.com.meushape.data.Feito
import br.com.meushape.data.ItemPlano
import br.com.meushape.data.Repo
import br.com.meushape.data.TipoItem
import br.com.meushape.logic.AtividadeFolga
import br.com.meushape.logic.Escala
import br.com.meushape.logic.Horario
import br.com.meushape.logic.TipoDia
import br.com.meushape.ui.CartaoApp
import br.com.meushape.ui.theme.Azul
import br.com.meushape.ui.theme.Cinza
import br.com.meushape.ui.theme.Laranja
import br.com.meushape.ui.theme.Limao
import br.com.meushape.ui.theme.Roxo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Aba Hoje com suas subtelas (calendário e edição da rotina). */
@Composable
fun HojeAba() {
    var tela by rememberSaveable { mutableStateOf("hoje") }
    BackHandler(enabled = tela != "hoje") { tela = "hoje" }
    when (tela) {
        "calendario" -> CalendarioScreen(onVoltar = { tela = "hoje" })
        "rotina" -> EditarRotinaScreen(onVoltar = { tela = "hoje" })
        else -> HojeScreen(
            abrirCalendario = { tela = "calendario" },
            abrirRotina = { tela = "rotina" },
        )
    }
}

fun corDoTipo(tipo: TipoDia): Color = when (tipo) {
    TipoDia.PLANTAO -> Laranja
    TipoDia.FOLGA -> Limao
    TipoDia.FERIAS -> Azul
    TipoDia.FERIADO -> Roxo
}

/** Título e descrição do item, considerando treino/caminhada da folga. */
fun textoItem(item: ItemPlano, atividade: AtividadeFolga?): Pair<String, String> =
    if (item.tipo == TipoItem.TREINO_FOLGA && atividade == AtividadeFolga.CAMINHADA && item.tituloAlt.isNotBlank())
        item.tituloAlt to item.descricaoAlt
    else item.titulo to item.descricao

private val FORMATO_DIA = DateTimeFormatter.ofPattern("EEEE, dd/MM", Locale("pt", "BR"))

@Composable
fun HojeScreen(abrirCalendario: () -> Unit, abrirRotina: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { Repo.get(ctx) }
    val scope = rememberCoroutineScope()

    // Relógio que atualiza a tela a cada 30 segundos.
    var agora by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); agora = LocalDateTime.now() } }

    val dia = Escala.diaLogico(agora)
    val escala by remember { repo.observarEscala() }.collectAsState(initial = null)
    val info = escala?.info(dia)
    val tipo = info?.tipo ?: TipoDia.FOLGA
    val planoBase by remember(tipo, escala == null) {
        if (escala == null) flowOf(emptyList()) else repo.observarPlano(tipo)
    }.collectAsState(initial = emptyList())
    val ajustesHoje by remember(dia) { repo.db.horarioDia().observarDia(dia.toString()) }.collectAsState(initial = emptyList())
    val plano = Repo.aplicarHorarios(planoBase, ajustesHoje)
    val alterados = ajustesHoje.map { it.itemId }.toSet()
    var mudandoHorario by remember { mutableStateOf<ItemPlano?>(null) }
    val feitosSemana by remember(dia) { repo.observarFeitosDaSemana(dia) }.collectAsState(initial = emptyList())
    val agua by remember(dia) { repo.db.agua().observar(dia.toString()) }.collectAsState(initial = null)
    val estoque by remember { repo.db.marmitas().observarEstoque() }.collectAsState(initial = emptyList())
    val sono by remember { repo.observarSonoConfirmado() }.collectAsState(initial = emptyList())
    val passos by remember(agora.toLocalDate()) { repo.db.sono().observarPassos(agora.toLocalDate().toString()) }
        .collectAsState(initial = null)

    // Sequência de dias cumprindo dieta e treino (recalcula quando algo é marcado).
    var sequencia by remember { mutableStateOf(0) }
    LaunchedEffect(feitosSemana, dia) { sequencia = repo.sequencia(dia) }

    val feitosHoje = feitosSemana.filter { it.data == dia.toString() }.associateBy { it.itemId }
    val livreUsada = feitosSemana.firstOrNull { it.refeicaoLivre }
    var escolhendoOpcao by remember { mutableStateOf<ItemPlano?>(null) }
    var confirmandoLivre by remember { mutableStateOf<ItemPlano?>(null) }

    fun marcar(item: ItemPlano, opcao: String = "", livre: Boolean = false) =
        scope.launch { repo.marcarFeito(dia, item.id, opcao, livre) }

    val proximoId = plano.firstOrNull {
        it.id !in feitosHoje && Horario.momento(dia, it.fimMin ?: it.inicioMin).isAfter(agora)
    }?.id

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.padding(top = 16.dp)) {
                Text(
                    dia.format(FORMATO_DIA).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium, color = Cinza,
                )
                if (info != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            info.tipo.nome, fontSize = 34.sp, fontWeight = FontWeight.Bold,
                            color = corDoTipo(info.tipo),
                        )
                        if (info.trocado) Text("  (trocado)", color = Cinza)
                        Spacer(Modifier.weight(1f))
                        Text("🔥 $sequencia", fontSize = 26.sp, fontWeight = FontWeight.Bold,
                            color = if (sequencia > 0) Laranja else Cinza)
                    }
                    Text(
                        when {
                            info.tipo == TipoDia.PLANTAO -> "Plantão das 17:40 às 05:40"
                            info.atividade == AtividadeFolga.TREINO -> "Hoje é dia de treino de manhã"
                            else -> "Hoje é dia de caminhada de manhã"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        if (sequencia == 0) "Sequência: cumpra todas as refeições e o treino de hoje para começar"
                        else "Sequência: $sequencia ${if (sequencia == 1) "dia" else "dias seguidos"} cumprindo dieta e treino",
                        color = Cinza, style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = abrirCalendario, modifier = Modifier.weight(1f)) { Text("Calendário") }
                FilledTonalButton(onClick = abrirRotina, modifier = Modifier.weight(1f)) { Text("Editar rotina") }
            }
        }
        val baixos = estoque.filter { it.total <= 2 }
        if (baixos.isNotEmpty()) {
            item {
                CartaoApp(cor = Laranja.copy(alpha = 0.2f)) {
                    Text("🍱 Estoque de marmitas baixo", fontWeight = FontWeight.SemiBold)
                    baixos.forEach {
                        Text("${if (it.tipo == "ALMOCO") "Almoço" else "Janta"}: ${it.total} restante(s)")
                    }
                    Text("Monte mais na aba Compras → Marmitas.", color = Cinza)
                }
            }
        }
        item {
            val sono24 = br.com.meushape.logic.SonoCalc.ultimas24h(sono, agora)
            val p = passos?.passos ?: 0
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CartaoApp(Modifier.weight(1f)) {
                    Text("👟 Passos", color = Cinza)
                    Text("%,d".format(p).replace(',', '.'), fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        color = if (p >= 8000) Limao else MaterialTheme.colorScheme.onSurface)
                    LinearProgressIndicator(progress = { (p / 8000f).coerceAtMost(1f) }, modifier = Modifier.fillMaxWidth())
                    Text("meta 8.000", color = Cinza, fontSize = 12.sp)
                }
                CartaoApp(Modifier.weight(1f)) {
                    Text("😴 Sono 24h", color = Cinza)
                    Text(horas(sono24), fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        color = if (sono24 < 420) Laranja else Limao)
                    LinearProgressIndicator(progress = { (sono24 / 420f).coerceAtMost(1f) },
                        color = if (sono24 < 420) Laranja else Limao, modifier = Modifier.fillMaxWidth())
                    Text(if (sono24 < 420) "abaixo de 7h" else "meta 7h", color = Cinza, fontSize = 12.sp)
                }
            }
        }
        item { CartaoAgua(agua, onMudar = { novo -> scope.launch { repo.db.agua().salvar(Agua(dia.toString(), novo)) } }) }
        item {
            val feitos = plano.count { it.id in feitosHoje }
            CartaoApp {
                Text("Rotina de hoje: $feitos de ${plano.size} feitos", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (plano.isEmpty()) 0f else feitos.toFloat() / plano.size },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (livreUsada != null) "Refeição livre da semana: já usada (${LocalDate.parse(livreUsada.data).format(DateTimeFormatter.ofPattern("dd/MM"))})"
                    else "Refeição livre da semana: disponível",
                    color = if (livreUsada != null) Cinza else Limao,
                )
            }
        }
        items(plano, key = { it.id }) { item ->
            ItemLinha(
                item = item,
                atividade = info?.atividade,
                feito = feitosHoje[item.id],
                destaque = item.id == proximoId,
                atrasado = item.id !in feitosHoje && Horario.momento(dia, item.fimMin ?: item.inicioMin).isBefore(agora),
                podeUsarLivre = livreUsada == null && item.tipo == TipoItem.REFEICAO,
                alteradoHoje = item.id in alterados,
                onHorario = { mudandoHorario = item },
                onMarcar = {
                    if (item.opcoes.isNotBlank()) escolhendoOpcao = item else marcar(item)
                },
                onDesmarcar = { scope.launch { repo.desmarcarFeito(dia, item.id) } },
                onLivre = { confirmandoLivre = item },
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    mudandoHorario?.let { item ->
        HorarioSoHojeDialog(
            item = item,
            original = planoBase.firstOrNull { it.id == item.id } ?: item,
            alterado = item.id in alterados,
            onDismiss = { mudandoHorario = null },
            onSalvar = { ini, fim ->
                mudandoHorario = null
                scope.launch {
                    repo.db.horarioDia().salvar(br.com.meushape.data.HorarioDia(dia.toString(), item.id, ini, fim))
                    br.com.meushape.notify.Alarmes.agendarProximo(ctx)
                }
            },
            onRestaurar = {
                mudandoHorario = null
                scope.launch {
                    repo.db.horarioDia().apagar(dia.toString(), item.id)
                    br.com.meushape.notify.Alarmes.agendarProximo(ctx)
                }
            },
        )
    }
    escolhendoOpcao?.let { item ->
        AlertDialog(
            onDismissRequest = { escolhendoOpcao = null },
            title = { Text("O que você comeu?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item.opcoes.split("|").map { it.trim() }.filter { it.isNotEmpty() }.forEach { opcao ->
                        OutlinedButton(
                            onClick = { marcar(item, opcao); escolhendoOpcao = null },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                        ) { Text(opcao) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { escolhendoOpcao = null }) { Text("Cancelar") } },
        )
    }
    confirmandoLivre?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmandoLivre = null },
            title = { Text("Usar a refeição livre?") },
            text = { Text("\"${item.titulo}\" vai ser marcada como a refeição livre desta semana. Só existe 1 por semana.") },
            confirmButton = {
                TextButton(onClick = { marcar(item, "Refeição livre", livre = true); confirmandoLivre = null }) { Text("Usar") }
            },
            dismissButton = { TextButton(onClick = { confirmandoLivre = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun CartaoAgua(agua: Agua?, onMudar: (Int) -> Unit) {
    val copos = agua?.copos ?: 0
    val metaCopos = 12 // 12 copos de 250 ml = 3 litros
    CartaoApp {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("💧 Água", fontWeight = FontWeight.SemiBold)
                Text("$copos de $metaCopos copos · %.2f L de 3 L".format(copos * 0.25), color = Cinza)
            }
            OutlinedButton(onClick = { if (copos > 0) onMudar(copos - 1) }, modifier = Modifier.size(56.dp)) { Text("−") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onMudar(copos + 1) }, modifier = Modifier.height(56.dp)) { Text("+1 copo") }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (copos.toFloat() / metaCopos).coerceAtMost(1f) },
            color = Azul,
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )
    }
}

@Composable
private fun ItemLinha(
    item: ItemPlano,
    atividade: AtividadeFolga?,
    feito: Feito?,
    destaque: Boolean,
    atrasado: Boolean,
    podeUsarLivre: Boolean,
    alteradoHoje: Boolean,
    onHorario: () -> Unit,
    onMarcar: () -> Unit,
    onDesmarcar: () -> Unit,
    onLivre: () -> Unit,
) {
    val (titulo, descricao) = textoItem(item, atividade)
    val horario = Horario.texto(item.inicioMin) + (item.fimMin?.let { " – " + Horario.texto(it) } ?: "")
    val borda = if (destaque) Modifier.border(2.dp, Limao, RoundedCornerShape(12.dp)) else Modifier
    CartaoApp(modifier = borda) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                // Tocar no horário muda só para hoje.
                Text(
                    horario + (if (alteradoHoje) "  (só hoje) ✎" else "  ✎"),
                    color = when { feito != null -> Limao; alteradoHoje -> Azul; atrasado -> Laranja; else -> Cinza },
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onHorario).padding(vertical = 4.dp),
                )
                Text(titulo, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (descricao.isNotBlank()) Text(descricao, style = MaterialTheme.typography.bodyMedium)
                if (feito != null && feito.opcao.isNotBlank()) Text("✓ ${feito.opcao}", color = Limao)
            }
            Spacer(Modifier.width(8.dp))
            Box {
                if (feito != null) {
                    Button(
                        onClick = onDesmarcar,
                        colors = ButtonDefaults.buttonColors(containerColor = Limao),
                        modifier = Modifier.size(64.dp),
                    ) { Text("✓", fontSize = 26.sp) }
                } else {
                    OutlinedButton(onClick = onMarcar, modifier = Modifier.size(64.dp)) { Text("", fontSize = 26.sp) }
                }
            }
        }
        if (feito == null && podeUsarLivre) {
            TextButton(onClick = onLivre) { Text("Usar como refeição livre") }
        }
    }
}

/** Muda o horário de um item só no dia de hoje. */
@Composable
private fun HorarioSoHojeDialog(
    item: ItemPlano,
    original: ItemPlano,
    alterado: Boolean,
    onDismiss: () -> Unit,
    onSalvar: (Int, Int?) -> Unit,
    onRestaurar: () -> Unit,
) {
    var ini by remember { mutableStateOf(item.inicioMin) }
    var fim by remember { mutableStateOf(item.fimMin) }
    var escolhendo by remember { mutableStateOf<String?>(null) }
    // Mantém a mesma duração ao mudar o início (ex.: treino de 1 hora).
    val duracao = original.fimMin?.let { it - original.inicioMin }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Horário de hoje: ${item.titulo}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Muda só hoje. A rotina dos outros dias continua igual.", color = Cinza)
                Text("Horário normal: ${Horario.texto(original.inicioMin)}" +
                    (original.fimMin?.let { " – ${Horario.texto(it)}" } ?: ""), color = Cinza)
                OutlinedButton(onClick = { escolhendo = "ini" }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Início: ${Horario.texto(ini)}", fontSize = 18.sp)
                }
                if (fim != null) {
                    OutlinedButton(onClick = { escolhendo = "fim" }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text("Fim: ${Horario.texto(fim!!)}", fontSize = 18.sp)
                    }
                }
                if (alterado) TextButton(onClick = onRestaurar) { Text("Voltar ao horário normal") }
            }
        },
        confirmButton = { TextButton(onClick = { onSalvar(ini, fim) }) { Text("Salvar para hoje") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
    escolhendo?.let { qual ->
        val atual = Math.floorMod(if (qual == "ini") ini else (fim ?: ini), 1440)
        br.com.meushape.ui.EscolherHora(atual / 60, atual % 60, onDismiss = { escolhendo = null }) { h, m ->
            val v = Horario.deTexto(h, m)
            if (qual == "ini") {
                ini = v
                if (duracao != null) fim = v + duracao
            } else fim = if (v <= ini) v + 1440 else v
            escolhendo = null
        }
    }
}

/** Abas ainda não construídas. */
@Composable
fun EmBreve(titulo: String, etapa: Int) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(titulo, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Chega na etapa $etapa.", color = Cinza)
        }
    }
}
