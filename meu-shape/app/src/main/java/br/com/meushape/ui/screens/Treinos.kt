package br.com.meushape.ui.screens

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.meushape.data.Exercicio
import br.com.meushape.data.Grupo
import br.com.meushape.data.Repo
import br.com.meushape.data.SerieFeita
import br.com.meushape.data.SessaoTreino
import br.com.meushape.data.Treino
import br.com.meushape.data.TreinoExercicio
import br.com.meushape.ui.CartaoApp
import br.com.meushape.ui.GraficoLinha
import br.com.meushape.ui.theme.Cinza
import br.com.meushape.ui.theme.Laranja
import br.com.meushape.ui.theme.Limao
import br.com.meushape.ui.theme.Vermelho
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formata carga: "20 kg", "22,5 kg". */
fun kg(v: Double): String =
    if (v % 1.0 == 0.0) "${v.toInt()} kg" else "%.1f kg".format(Locale("pt", "BR"), v)

private fun lerNumero(t: String): Double? = t.trim().replace(",", ".").toDoubleOrNull()

private fun numeroTexto(v: Double): String =
    if (v == 0.0) "" else if (v % 1.0 == 0.0) v.toInt().toString() else v.toString().replace(".", ",")

/** Aba Treinos com suas subtelas. */
@Composable
fun TreinosAba() {
    val ctx = LocalContext.current
    val repo = remember { Repo.get(ctx) }
    val aberta by remember { repo.db.treinos().observarSessaoAberta() }.collectAsState(initial = null)
    var tela by rememberSaveable { mutableStateOf("lista") }
    var alvo by rememberSaveable { mutableLongStateOf(0L) }
    BackHandler(enabled = tela != "lista") { tela = "lista" }

    when (tela) {
        "editar" -> EditarTreinoScreen(alvo, onVoltar = { tela = "lista" })
        "sessao" -> SessaoScreen(alvo, onSair = { tela = "lista" })
        "historico" -> HistoricoScreen(onVoltar = { tela = "lista" })
        else -> ListaTreinosScreen(
            sessaoAberta = aberta,
            onEditar = { alvo = it; tela = "editar" },
            onSessao = { alvo = it; tela = "sessao" },
            onHistorico = { tela = "historico" },
        )
    }
}

@Composable
private fun ListaTreinosScreen(
    sessaoAberta: SessaoTreino?,
    onEditar: (Long) -> Unit,
    onSessao: (Long) -> Unit,
    onHistorico: () -> Unit,
) {
    val ctx = LocalContext.current
    val repo = remember { Repo.get(ctx) }
    val scope = rememberCoroutineScope()
    val treinos by remember { repo.db.treinos().observarTreinos() }.collectAsState(initial = emptyList())
    val itens by remember { repo.db.treinos().observarTodosItens() }.collectAsState(initial = emptyList())
    val exercicios by remember { repo.db.treinos().observarExercicios() }.collectAsState(initial = emptyList())
    val nomes = exercicios.associate { it.id to it.nome }
    var explicandoAdaptacao by remember { mutableStateOf(false) }
    val temAdaptacao = treinos.any { it.nome == "Adaptação A" } && treinos.any { it.nome == "Adaptação B" }
    var apagando by remember { mutableStateOf<Treino?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("Treinos", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            if (temAdaptacao) {
                TextButton(onClick = { explicandoAdaptacao = true }) { Text("Como fazer a adaptação?") }
            }
        }
        if (sessaoAberta != null) {
            item {
                val nome = treinos.firstOrNull { it.id == sessaoAberta.treinoId }?.nome ?: ""
                CartaoApp(cor = Limao.copy(alpha = 0.18f)) {
                    Text("Treino $nome em andamento", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onSessao(sessaoAberta.id) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text("Continuar treino")
                    }
                }
            }
        }
        if (!temAdaptacao) {
            item {
                CartaoApp(cor = Limao.copy(alpha = 0.12f)) {
                    Text("Ainda não tem um treino montado?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Comece pelo treino de adaptação pronto: corpo inteiro, em duas partes (A e B) para alternar. " +
                        "Se apagou um deles, aqui você cria de novo.")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { explicandoAdaptacao = true }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text("Usar treino de adaptação")
                    }
                }
            }
        }
        if (treinos.isEmpty()) {
            item {
                Text(
                    "Ou monte seus treinos (A, B, C...) com os exercícios que o instrutor passar.",
                    color = Cinza,
                )
            }
        }
        items(treinos, key = { it.id }) { t ->
            val doTreino = itens.filter { it.treinoId == t.id }
            CartaoApp {
                Text(if (t.nome.startsWith("Adaptação")) t.nome else "Treino ${t.nome}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (t.observacao.isNotBlank()) Text(t.observacao, color = Cinza)
                Text(
                    if (doTreino.isEmpty()) "Nenhum exercício"
                    else doTreino.joinToString(" · ") { nomes[it.exercicioId] ?: "?" },
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { scope.launch { onSessao(repo.comecarTreino(t.id)) } },
                        enabled = sessaoAberta == null && doTreino.isNotEmpty(),
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) { Text("Começar") }
                    OutlinedButton(onClick = { onEditar(t.id) }, modifier = Modifier.weight(1f).height(56.dp)) { Text("Editar") }
                }
                TextButton(onClick = { apagando = t }) { Text("Apagar este treino", color = Vermelho) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            val proxima = ('A' + treinos.size).toString()
                            val id = repo.db.treinos().salvarTreino(Treino(nome = proxima, ordem = treinos.size))
                            onEditar(id)
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                ) { Text("+ Novo treino") }
                FilledTonalButton(onClick = onHistorico, modifier = Modifier.weight(1f).height(56.dp)) { Text("Histórico") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    apagando?.let { t ->
        AlertDialog(
            onDismissRequest = { apagando = null },
            title = { Text("Apagar ${if (t.nome.startsWith("Adaptação")) t.nome else "o treino " + t.nome}?") },
            text = { Text("O treino sai da lista. O histórico de cargas dos exercícios continua guardado.") },
            confirmButton = {
                TextButton(onClick = {
                    apagando = null
                    scope.launch {
                        if (sessaoAberta?.treinoId == t.id) repo.descartarTreino(sessaoAberta.id)
                        repo.db.treinos().limparItens(t.id)
                        repo.db.treinos().apagarTreino(t)
                    }
                }) { Text("Apagar", color = Vermelho) }
            },
            dismissButton = { TextButton(onClick = { apagando = null }) { Text("Cancelar") } },
        )
    }
    if (explicandoAdaptacao) {
        AdaptacaoDialog(
            jaCriado = temAdaptacao,
            onDismiss = { explicandoAdaptacao = false },
            onCriar = { explicandoAdaptacao = false; scope.launch { repo.criarTreinosAdaptacao() } },
        )
    }
}

@Composable
private fun AdaptacaoDialog(jaCriado: Boolean, onDismiss: () -> Unit, onCriar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Treino de adaptação") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(br.com.meushape.data.TreinoAdaptacao.EXPLICACAO)
                Spacer(Modifier.height(10.dp))
                Text("Adaptação A", fontWeight = FontWeight.Bold)
                br.com.meushape.data.TreinoAdaptacao.A.forEach { Text("• ${it.exercicio} — ${it.series}×${it.reps}", color = Cinza) }
                Spacer(Modifier.height(6.dp))
                Text("Adaptação B", fontWeight = FontWeight.Bold)
                br.com.meushape.data.TreinoAdaptacao.B.forEach { Text("• ${it.exercicio} — ${it.series}×${it.reps}", color = Cinza) }
            }
        },
        confirmButton = {
            if (!jaCriado) TextButton(onClick = onCriar) { Text("Criar treinos A e B") }
            else TextButton(onClick = onDismiss) { Text("OK") }
        },
        dismissButton = { if (!jaCriado) TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

// ---------------- Montar treino ----------------

@Composable
private fun EditarTreinoScreen(treinoId: Long, onVoltar: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { Repo.get(ctx) }
    val scope = rememberCoroutineScope()
    val dao = repo.db.treinos()
    val treinos by remember { dao.observarTreinos() }.collectAsState(initial = emptyList())
    val treino = treinos.firstOrNull { it.id == treinoId }
    val itens by remember(treinoId) { dao.observarItens(treinoId) }.collectAsState(initial = emptyList())
    val exercicios by remember { dao.observarExercicios() }.collectAsState(initial = emptyList())
    val porId = exercicios.associateBy { it.id }
    var escolhendo by remember { mutableStateOf(false) }
    var editandoItem by remember { mutableStateOf<TreinoExercicio?>(null) }
    var renomeando by remember { mutableStateOf(false) }
    var apagando by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            TextButton(onClick = onVoltar) { Text("← Voltar") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Treino ${treino?.nome ?: ""}", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { renomeando = true }) { Text("Renomear") }
            }
            if (!treino?.observacao.isNullOrBlank()) Text(treino!!.observacao, color = Cinza)
        }
        items(itens, key = { it.id }) { item ->
            val ex = porId[item.exercicioId]
            CartaoApp(Modifier.clickable { editandoItem = item }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(ex?.nome ?: "?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "${item.series} × ${item.repeticoes}" +
                                (if (item.carga > 0) " · ${kg(item.carga)}" else "") +
                                " · descanso ${item.descansoSeg}s",
                            color = Cinza,
                        )
                        Text(ex?.grupo ?: "", color = Cinza, style = MaterialTheme.typography.bodySmall)
                    }
                    Column {
                        TextButton(onClick = { scope.launch { mover(repo, itens, item, -1) } }) { Text("▲") }
                        TextButton(onClick = { scope.launch { mover(repo, itens, item, +1) } }) { Text("▼") }
                    }
                }
            }
        }
        item {
            Button(onClick = { escolhendo = true }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("+ Adicionar exercícios")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { apagando = true }) { Text("Apagar este treino", color = Vermelho) }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (escolhendo) {
        EscolherExerciciosDialog(
            exercicios = exercicios,
            jaNoTreino = itens.map { it.exercicioId }.toSet(),
            onDismiss = { escolhendo = false },
            onNovo = { nome, grupo -> scope.launch { dao.salvarExercicio(Exercicio(nome = nome, grupo = grupo, personalizado = true)) } },
            onConfirmar = { ids ->
                escolhendo = false
                scope.launch {
                    var ordem = (itens.maxOfOrNull { it.ordem } ?: -1) + 1
                    ids.forEach { id ->
                        val cardio = porId[id]?.grupo == "Cardio"
                        dao.salvarItem(
                            TreinoExercicio(
                                treinoId = treinoId, exercicioId = id, ordem = ordem++,
                                series = if (cardio) 1 else 3, repeticoes = if (cardio) "20 min" else "10-12",
                            )
                        )
                    }
                }
            },
        )
    }
    editandoItem?.let { item ->
        EditarItemTreinoDialog(
            item = item,
            nome = porId[item.exercicioId]?.nome ?: "",
            onDismiss = { editandoItem = null },
            onSalvar = { editandoItem = null; scope.launch { dao.salvarItem(it) } },
            onRemover = { editandoItem = null; scope.launch { dao.apagarItem(item) } },
        )
    }
    if (renomeando && treino != null) {
        var nome by remember { mutableStateOf(treino.nome) }
        var obs by remember { mutableStateOf(treino.observacao) }
        AlertDialog(
            onDismissRequest = { renomeando = false },
            title = { Text("Nome do treino") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(nome, { nome = it.take(20) }, label = { Text("Nome (ex.: A, B, Peito)") }, singleLine = true)
                    OutlinedTextField(obs, { obs = it }, label = { Text("Observação (ex.: peito e tríceps)") })
                }
            },
            confirmButton = {
                TextButton(enabled = nome.isNotBlank(), onClick = {
                    renomeando = false
                    scope.launch { dao.salvarTreino(treino.copy(nome = nome.trim(), observacao = obs.trim())) }
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { renomeando = false }) { Text("Cancelar") } },
        )
    }
    if (apagando && treino != null) {
        AlertDialog(
            onDismissRequest = { apagando = false },
            title = { Text("Apagar o treino ${treino.nome}?") },
            text = { Text("O histórico de cargas dos exercícios continua guardado.") },
            confirmButton = {
                TextButton(onClick = {
                    apagando = false
                    scope.launch { dao.limparItens(treino.id); dao.apagarTreino(treino); onVoltar() }
                }) { Text("Apagar", color = Vermelho) }
            },
            dismissButton = { TextButton(onClick = { apagando = false }) { Text("Cancelar") } },
        )
    }
}

private suspend fun mover(repo: Repo, itens: List<TreinoExercicio>, item: TreinoExercicio, direcao: Int) {
    val i = itens.indexOf(item)
    val j = i + direcao
    if (i < 0 || j !in itens.indices) return
    val nova = itens.toMutableList().apply { add(j, removeAt(i)) }
    nova.forEachIndexed { idx, it -> if (it.ordem != idx) repo.db.treinos().salvarItem(it.copy(ordem = idx)) }
}

@Composable
private fun EscolherExerciciosDialog(
    exercicios: List<Exercicio>,
    jaNoTreino: Set<Long>,
    onDismiss: () -> Unit,
    onNovo: (String, String) -> Unit,
    onConfirmar: (List<Long>) -> Unit,
) {
    var grupo by remember { mutableStateOf(Grupo.TODOS.first()) }
    var busca by remember { mutableStateOf("") }
    val marcados = remember { mutableStateMapOf<Long, Boolean>() }
    var criando by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escolha os exercícios") },
        text = {
            Column {
                OutlinedTextField(busca, { busca = it }, label = { Text("Buscar") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (busca.isBlank()) {
                    Row(Modifier.horizontalScrollCompat(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Grupo.TODOS.forEach { g -> FilterChip(grupo == g, { grupo = g }, { Text(g) }) }
                    }
                }
                val lista = exercicios.filter {
                    if (busca.isNotBlank()) it.nome.contains(busca.trim(), ignoreCase = true) else it.grupo == grupo
                }
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(lista, key = { it.id }) { ex ->
                        val ja = ex.id in jaNoTreino
                        Row(
                            Modifier.fillMaxWidth().clickable(enabled = !ja) { marcados[ex.id] = !(marcados[ex.id] ?: false) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = ja || marcados[ex.id] == true, onCheckedChange = null, enabled = !ja)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(ex.nome + if (ex.personalizado) " ★" else "")
                                if (busca.isNotBlank()) Text(ex.grupo, color = Cinza, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        HorizontalDivider()
                    }
                }
                TextButton(onClick = { criando = true }) { Text("+ Exercício que não está na lista") }
            }
        },
        confirmButton = {
            val ids = marcados.filterValues { it }.keys.toList()
            TextButton(enabled = ids.isNotEmpty(), onClick = { onConfirmar(ids) }) { Text("Adicionar (${ids.size})") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )

    if (criando) {
        var nome by remember { mutableStateOf(busca) }
        var g by remember { mutableStateOf(grupo) }
        AlertDialog(
            onDismissRequest = { criando = false },
            title = { Text("Novo exercício") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(nome, { nome = it }, label = { Text("Nome") }, singleLine = true)
                    Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                        Grupo.TODOS.forEach { opcao ->
                            Row(Modifier.fillMaxWidth().clickable { g = opcao }.padding(vertical = 4.dp)) {
                                Text((if (g == opcao) "● " else "○ ") + opcao)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = nome.isNotBlank(), onClick = {
                    onNovo(nome.trim(), g); grupo = g; busca = ""; criando = false
                }) { Text("Criar") }
            },
            dismissButton = { TextButton(onClick = { criando = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Modifier.horizontalScrollCompat(): Modifier =
    this.padding(vertical = 4.dp).horizontalScroll(rememberScrollState())

@Composable
private fun EditarItemTreinoDialog(
    item: TreinoExercicio,
    nome: String,
    onDismiss: () -> Unit,
    onSalvar: (TreinoExercicio) -> Unit,
    onRemover: () -> Unit,
) {
    var series by remember { mutableIntStateOf(item.series) }
    var reps by remember { mutableStateOf(item.repeticoes) }
    var carga by remember { mutableStateOf(numeroTexto(item.carga)) }
    var descanso by remember { mutableIntStateOf(item.descansoSeg) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(nome) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Séries")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { if (series > 1) series-- }, modifier = Modifier.size(52.dp)) { Text("−") }
                    Text("$series", Modifier.width(56.dp), fontSize = 24.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = { if (series < 10) series++ }, modifier = Modifier.size(52.dp)) { Text("+") }
                }
                OutlinedTextField(reps, { reps = it }, label = { Text("Repetições (ex.: 10-12, 15, 20 min)") }, singleLine = true)
                OutlinedTextField(
                    carga, { carga = it }, label = { Text("Carga (kg)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Text("Descanso")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(30, 45, 60, 90, 120).forEach { s ->
                        FilterChip(descanso == s, { descanso = s }, { Text("${s}s") })
                    }
                }
                TextButton(onClick = onRemover) { Text("Tirar do treino", color = Vermelho) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSalvar(item.copy(series = series, repeticoes = reps.trim().ifEmpty { "10" }, carga = lerNumero(carga) ?: 0.0, descansoSeg = descanso))
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

// ---------------- Treino em andamento ----------------

@Composable
private fun SessaoScreen(sessaoId: Long, onSair: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { Repo.get(ctx) }
    val scope = rememberCoroutineScope()
    val dao = repo.db.treinos()
    val sessao by remember(sessaoId) { dao.observarSessao(sessaoId) }.collectAsState(initial = null)
    val s = sessao
    val treinos by remember { dao.observarTreinos() }.collectAsState(initial = emptyList())
    val exercicios by remember { dao.observarExercicios() }.collectAsState(initial = emptyList())
    val porId = exercicios.associateBy { it.id }
    val itens by remember(s?.treinoId) { dao.observarItens(s?.treinoId ?: -1) }.collectAsState(initial = emptyList())
    val feitas by remember(sessaoId) { dao.observarSeries(sessaoId) }.collectAsState(initial = emptyList())
    var ultimas by remember { mutableStateOf<Map<Long, SerieFeita>>(emptyMap()) }
    LaunchedEffect(Unit) { ultimas = dao.ultimasSeries().associateBy { it.exercicioId } }

    // Campos de carga/repetições digitados (chave = "exercicio-serie").
    val cargas = remember { mutableStateMapOf<String, String>() }
    val repsCampo = remember { mutableStateMapOf<String, String>() }

    // Cronômetro de descanso: guarda o momento em que termina.
    var descansoAte by rememberSaveable { mutableLongStateOf(0L) }
    var agora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var confirmarFim by remember { mutableStateOf(false) }
    var confirmarDescarte by remember { mutableStateOf(false) }

    // Mantém a tela acesa durante o treino.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(descansoAte) {
        while (descansoAte > System.currentTimeMillis()) {
            agora = System.currentTimeMillis(); delay(250)
        }
        agora = System.currentTimeMillis()
        if (descansoAte != 0L) {
            vibrar(ctx)
            descansoAte = 0L
        }
    }
    var tempoTreino by remember { mutableLongStateOf(0L) }
    LaunchedEffect(s?.inicio) {
        while (true) { tempoTreino = System.currentTimeMillis() - (s?.inicio ?: System.currentTimeMillis()); delay(1000) }
    }

    if (s == null) {
        Text("Carregando...", Modifier.padding(24.dp)); return
    }
    val treino = treinos.firstOrNull { it.id == s.treinoId }
    val restante = ((descansoAte - agora) / 1000).coerceAtLeast(0)

    Column(Modifier.fillMaxSize()) {
        // Cabeçalho fixo com o cronômetro de descanso.
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSair) { Text("← Sair") }
                Text(
                    "Treino ${treino?.nome ?: ""} · ${tempoTreino / 60000} min",
                    fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f),
                )
            }
            if (descansoAte > agora) {
                CartaoApp(cor = Laranja.copy(alpha = 0.25f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Descanso  %d:%02d".format(restante / 60, restante % 60), fontSize = 28.sp,
                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { descansoAte += 15_000 }) { Text("+15s") }
                        TextButton(onClick = { descansoAte = 0L }) { Text("Pular") }
                    }
                }
            }
        }
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(itens, key = { it.id }) { item ->
                val ex = porId[item.exercicioId]
                val feitasEx = feitas.filter { it.exercicioId == item.exercicioId }.associateBy { it.numero }
                val sugestao = ultimas[item.exercicioId]?.carga ?: item.carga
                CartaoApp {
                    Text(ex?.nome ?: "?", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${item.series} × ${item.repeticoes}" +
                            (ultimas[item.exercicioId]?.let { " · última vez ${kg(it.carga)}" } ?: ""),
                        color = Cinza,
                    )
                    Spacer(Modifier.height(6.dp))
                    for (n in 1..item.series) {
                        val chave = "${item.exercicioId}-$n"
                        val feita = feitasEx[n]
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                            Text("$n", Modifier.width(24.dp), fontWeight = FontWeight.Bold, color = if (feita != null) Limao else Cinza)
                            OutlinedTextField(
                                value = feita?.let { numeroTexto(it.carga) } ?: (cargas[chave] ?: numeroTexto(sugestao)),
                                onValueChange = { cargas[chave] = it },
                                enabled = feita == null,
                                label = { Text("kg") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(6.dp))
                            OutlinedTextField(
                                value = feita?.repeticoes?.toString() ?: (repsCampo[chave] ?: (item.repeticoes.takeWhile { it.isDigit() })),
                                onValueChange = { repsCampo[chave] = it.filter { c -> c.isDigit() } },
                                enabled = feita == null,
                                label = { Text("reps") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(6.dp))
                            if (feita != null) {
                                Button(
                                    onClick = { scope.launch { dao.apagarSerie(sessaoId, item.exercicioId, n) } },
                                    colors = ButtonDefaults.buttonColors(containerColor = Limao),
                                    modifier = Modifier.size(56.dp),
                                ) { Text("✓", fontSize = 22.sp) }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        val c = lerNumero(cargas[chave] ?: numeroTexto(sugestao)) ?: 0.0
                                        val r = (repsCampo[chave] ?: item.repeticoes.takeWhile { it.isDigit() }).toIntOrNull() ?: 0
                                        scope.launch {
                                            dao.inserirSerie(SerieFeita(sessaoId = sessaoId, exercicioId = item.exercicioId,
                                                numero = n, carga = c, repeticoes = r, feitaEm = System.currentTimeMillis()))
                                        }
                                        descansoAte = System.currentTimeMillis() + item.descansoSeg * 1000L
                                    },
                                    modifier = Modifier.size(56.dp),
                                ) { Text("") }
                            }
                        }
                    }
                }
            }
            item {
                val total = itens.sumOf { it.series }
                Text("${feitas.size} de $total séries feitas", color = Cinza)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { confirmarFim = true }, modifier = Modifier.fillMaxWidth().height(60.dp)) {
                    Text("Finalizar treino", fontSize = 18.sp)
                }
                TextButton(onClick = { confirmarDescarte = true }) { Text("Descartar este treino", color = Vermelho) }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (confirmarFim) {
        AlertDialog(
            onDismissRequest = { confirmarFim = false },
            title = { Text("Finalizar treino?") },
            text = { Text("${feitas.size} séries registradas. O treino de hoje será marcado como feito na tela Hoje.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarFim = false
                    scope.launch { repo.encerrarTreino(s); onSair() }
                }) { Text("Finalizar") }
            },
            dismissButton = { TextButton(onClick = { confirmarFim = false }) { Text("Continuar treinando") } },
        )
    }
    if (confirmarDescarte) {
        AlertDialog(
            onDismissRequest = { confirmarDescarte = false },
            title = { Text("Descartar treino?") },
            text = { Text("As séries registradas nesta sessão serão apagadas.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarDescarte = false
                    scope.launch { repo.descartarTreino(sessaoId); onSair() }
                }) { Text("Descartar", color = Vermelho) }
            },
            dismissButton = { TextButton(onClick = { confirmarDescarte = false }) { Text("Cancelar") } },
        )
    }
}

@Suppress("DEPRECATION")
private fun vibrar(ctx: android.content.Context) {
    try {
        val v = ctx.getSystemService(Vibrator::class.java) ?: return
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
    } catch (_: Exception) {
    }
}

// ---------------- Histórico ----------------

@Composable
private fun HistoricoScreen(onVoltar: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { Repo.get(ctx) }
    val dao = repo.db.treinos()
    val exercicios by remember { dao.observarExercicios() }.collectAsState(initial = emptyList())
    val comHistorico by remember { dao.observarExerciciosComHistorico() }.collectAsState(initial = emptyList())
    var selecionado by rememberSaveable { mutableLongStateOf(0L) }
    BackHandler(enabled = selecionado != 0L) { selecionado = 0L }

    if (selecionado != 0L) {
        val ex = exercicios.firstOrNull { it.id == selecionado }
        val pontos by remember(selecionado) { dao.observarHistorico(selecionado) }.collectAsState(initial = emptyList())
        val fmt = DateTimeFormatter.ofPattern("dd/MM")
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                TextButton(onClick = { selecionado = 0L }) { Text("← Voltar") }
                Text(ex?.nome ?: "", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Evolução da maior carga em cada treino", color = Cinza)
            }
            item {
                CartaoApp {
                    GraficoLinha(pontos.map { LocalDate.parse(it.data).format(fmt) to it.cargaMax }, unidade = " kg")
                }
            }
            if (pontos.size >= 2) {
                item {
                    val dif = pontos.last().cargaMax - pontos.first().cargaMax
                    Text(
                        if (dif >= 0) "Subiu ${kg(dif)} desde o primeiro registro 💪" else "Caiu ${kg(-dif)} desde o primeiro registro",
                        color = if (dif >= 0) Limao else Laranja,
                    )
                }
            }
            items(pontos.reversed()) { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(LocalDate.parse(p.data).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), Modifier.weight(1f))
                    Text("${kg(p.cargaMax)} · ${p.series} séries", color = Cinza)
                }
                HorizontalDivider()
            }
        }
        return
    }

    val scope = rememberCoroutineScope()
    val sessoes by remember { dao.observarSessoesFeitas() }.collectAsState(initial = emptyList())
    val treinos by remember { dao.observarTreinos() }.collectAsState(initial = emptyList())
    var apagandoSessao by remember { mutableStateOf<SessaoTreino?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            TextButton(onClick = onVoltar) { Text("← Voltar") }
            Text("Treinos feitos", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Registrou no treino errado? Apague aqui.", color = Cinza)
        }
        if (sessoes.isEmpty()) item { Text("Nenhum treino finalizado ainda.", color = Cinza) }
        items(sessoes, key = { "s" + it.id }) { s ->
            val nome = treinos.firstOrNull { it.id == s.treinoId }?.nome ?: "apagado"
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${LocalDate.parse(s.data).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} · $nome", fontWeight = FontWeight.SemiBold)
                    s.fim?.let { Text("${(it - s.inicio) / 60000} min", color = Cinza) }
                }
                TextButton(onClick = { apagandoSessao = s }) { Text("Apagar", color = Vermelho) }
            }
            HorizontalDivider()
        }
        item {
            Spacer(Modifier.height(12.dp))
            Text("Histórico por exercício", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        val lista = exercicios.filter { it.id in comHistorico }
        if (lista.isEmpty()) item { Text("Ainda não há séries registradas. Faça um treino!", color = Cinza) }
        items(lista, key = { it.id }) { ex ->
            CartaoApp(Modifier.clickable { selecionado = ex.id }) {
                Text(ex.nome, fontWeight = FontWeight.Bold)
                Text(ex.grupo, color = Cinza)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
    apagandoSessao?.let { s ->
        AlertDialog(
            onDismissRequest = { apagandoSessao = null },
            title = { Text("Apagar este treino feito?") },
            text = { Text("As séries e cargas registradas nele serão apagadas do histórico.") },
            confirmButton = {
                TextButton(onClick = {
                    apagandoSessao = null
                    scope.launch { repo.descartarTreino(s.id) }
                }) { Text("Apagar", color = Vermelho) }
            },
            dismissButton = { TextButton(onClick = { apagandoSessao = null }) { Text("Cancelar") } },
        )
    }
}
