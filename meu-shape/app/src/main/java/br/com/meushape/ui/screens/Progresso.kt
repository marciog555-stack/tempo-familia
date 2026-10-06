package br.com.meushape.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import br.com.meushape.data.Angulo
import br.com.meushape.data.Backup
import br.com.meushape.data.Cintura
import br.com.meushape.data.Config
import br.com.meushape.data.Foto
import br.com.meushape.data.Peso
import br.com.meushape.data.Repo
import br.com.meushape.logic.ProgressoCalc
import br.com.meushape.notify.Alarmes
import br.com.meushape.notify.ResumoSemanal
import br.com.meushape.ui.CartaoApp
import br.com.meushape.ui.GraficoLinha
import br.com.meushape.ui.theme.Azul
import br.com.meushape.ui.theme.Cinza
import br.com.meushape.ui.theme.Laranja
import br.com.meushape.ui.theme.Limao
import br.com.meushape.ui.theme.Vermelho
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val BR = Locale("pt", "BR")
private val FMT_DM = DateTimeFormatter.ofPattern("dd/MM")
private val FMT_DMA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private fun num(v: Double, casas: Int = 1) = "%.${casas}f".format(BR, v)
private fun lerDecimal(t: String) = t.trim().replace(",", ".").toDoubleOrNull()

@Composable
fun ProgressoAba() {
    var aba by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = aba, edgePadding = 8.dp) {
            listOf("Peso", "Cintura e fotos", "Resumo", "Perfil e backup").forEachIndexed { i, t ->
                Tab(aba == i, { aba = i }, text = { Text(t) })
            }
        }
        when (aba) {
            0 -> PesoTela()
            1 -> CinturaFotosTela()
            2 -> ResumoTela()
            else -> PerfilBackupTela()
        }
    }
}

/** Lê o perfil (configurações) como mapa. */
@Composable
private fun perfil(repo: Repo): Map<String, String> {
    val cfg by remember { repo.db.config().observar().map { l -> l.associate { it.chave to it.valor } } }
        .collectAsState(initial = emptyMap())
    return cfg
}

// ---------------- Peso ----------------

@Composable
private fun PesoTela() {
    val ctx = LocalContext.current
    val repo = remember { Repo.get(ctx) }
    val scope = rememberCoroutineScope()
    val pesos by remember { repo.db.progresso().observarPesos() }.collectAsState(initial = emptyList())
    val cfg = perfil(repo)
    var registrando by remember { mutableStateOf<Peso?>(null) }

    val hoje = LocalDate.now()
    val inicial = cfg["peso_inicial"]?.toDoubleOrNull() ?: 74.2
    val metaMin = cfg["meta_peso_min"]?.toDoubleOrNull() ?: 64.0
    val metaMax = cfg["meta_peso_max"]?.toDoubleOrNull() ?: 65.0
    val meta = (metaMin + metaMax) / 2
    val dataMeta = cfg["meta_data"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.of(2027, 2, 6)
    val dataInicial = cfg["data_inicial"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.of(2026, 10, 6)
    val lista = pesos.map { LocalDate.parse(it.data) to it.kg }
    val medias = ProgressoCalc.mediasSemanais(lista)
    val atual = medias.lastOrNull()?.media ?: inicial
    val ritmo = ProgressoCalc.ritmoNecessario(atual, metaMax, hoje, dataMeta)
    val plato = ProgressoCalc.plato(medias)
    val pesoHoje = pesos.firstOrNull { it.data == hoje.toString() }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Spacer(Modifier.height(12.dp))
            CartaoApp {
                Text("Média desta semana", color = Cinza)
                Text("${num(atual)} kg", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Limao)
                Text("Início: ${num(inicial)} kg em ${dataInicial.format(FMT_DM)} · perdeu ${num(inicial - atual)} kg")
                Text("Meta: ${num(metaMin, 0)}–${num(metaMax, 0)} kg até ${dataMeta.format(FMT_DMA)}")
                val faltam = atual - metaMax
                if (faltam > 0) {
                    Text("Faltam ${num(faltam)} kg" + (ritmo?.let { " · ritmo necessário ${num(it, 2)} kg/semana" } ?: ""), color = Cinza)
                } else Text("Meta alcançada! 🎉", color = Limao)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { registrando = pesoHoje ?: Peso(hoje.toString(), lista.lastOrNull()?.second ?: inicial) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text(if (pesoHoje != null) "Peso de hoje: ${num(pesoHoje.kg)} kg (editar)" else "Registrar peso de hoje (em jejum)") }
            }
        }
        if (plato) {
            item {
                CartaoApp(cor = Laranja.copy(alpha = 0.2f)) {
                    Text("⚠️ Peso parado há 2 semanas", fontWeight = FontWeight.Bold)
                    Text("A média semanal quase não mudou. Sugestão: corte 100 a 150 kcal por dia " +
                        "(ex.: metade do arroz da janta) ou aumente os passos diários.")
                }
            }
        }
        item {
            CartaoApp {
                Text("Evolução (linha tracejada = meta)", color = Cinza)
                Spacer(Modifier.height(6.dp))
                val pontos = (listOf(dataInicial to inicial) + lista).distinctBy { it.first }.sortedBy { it.first }
                    .takeLast(90).map { it.first.format(FMT_DM) to it.second }
                GraficoLinha(pontos, meta = meta, unidade = " kg")
            }
        }
        if (medias.isNotEmpty()) {
            item { Text("Médias semanais", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) }
            items(medias.reversed().take(12)) { m ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("Semana de ${m.segunda.format(FMT_DM)}", Modifier.weight(1f))
                    Text("${num(m.media)} kg (${m.registros} dias)", color = Cinza)
                }
                HorizontalDivider()
            }
        }
        item { Text("Registros", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) }
        items(pesos.reversed().take(30), key = { it.data }) { p ->
            Row(Modifier.fillMaxWidth().clickable { registrando = p }.padding(vertical = 8.dp)) {
                Text(LocalDate.parse(p.data).format(FMT_DMA), Modifier.weight(1f))
                Text("${num(p.kg)} kg", fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider()
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    registrando?.let { p ->
        NumeroDialog(
            titulo = "Peso de ${LocalDate.parse(p.data).format(FMT_DM)}", unidade = "kg", inicial = p.kg,
            onDismiss = { registrando = null },
            onSalvar = { v -> registrando = null; scope.launch { repo.db.progresso().salvarPeso(p.copy(kg = v)) } },
            onApagar = if (pesos.any { it.data == p.data }) {
                { registrando = null; scope.launch { repo.db.progresso().apagarPeso(p) } }
            } else null,
            onTrocarData = { nova -> registrando = p.copy(data = nova.toString()) },
            data = LocalDate.parse(p.data),
        )
    }
}

@Composable
private fun NumeroDialog(
    titulo: String,
    unidade: String,
    inicial: Double,
    onDismiss: () -> Unit,
    onSalvar: (Double) -> Unit,
    onApagar: (() -> Unit)?,
    data: LocalDate? = null,
    onTrocarData: ((LocalDate) -> Unit)? = null,
) {
    var texto by remember(titulo) { mutableStateOf(num(inicial)) }
    val valor = lerDecimal(texto)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { valor?.let { texto = num(it - 0.1) } }) { Text("−0,1") }
                    OutlinedTextField(
                        texto, { texto = it }, singleLine = true, suffix = { Text(unidade) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                    )
                    OutlinedButton(onClick = { valor?.let { texto = num(it + 0.1) } }) { Text("+0,1") }
                }
                if (data != null && onTrocarData != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = { onTrocarData(data.minusDays(1)) }) { Text("← dia anterior") }
                        if (data.isBefore(LocalDate.now())) TextButton(onClick = { onTrocarData(data.plusDays(1)) }) { Text("dia seguinte →") }
                    }
                }
                if (onApagar != null) TextButton(onClick = onApagar) { Text("Apagar", color = Vermelho) }
            }
        },
        confirmButton = { TextButton(enabled = valor != null && valor > 0, onClick = { onSalvar(valor!!) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

// ---------------- Cintura e fotos ----------------

private fun carregarMiniatura(arquivo: String, lado: Int = 700): Bitmap? = try {
    val op = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(arquivo, op)
    var amostra = 1
    while (op.outWidth / (amostra * 2) >= lado && op.outHeight / (amostra * 2) >= lado) amostra *= 2
    BitmapFactory.decodeFile(arquivo, BitmapFactory.Options().apply { inSampleSize = amostra })
} catch (_: Exception) { null }

@Composable
private fun FotoView(foto: Foto?, modifier: Modifier = Modifier) {
    var bmp by remember(foto?.arquivo) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(foto?.arquivo) {
        bmp = foto?.let { withContext(Dispatchers.IO) { carregarMiniatura(it.arquivo) } }
    }
    Box(modifier.aspectRatio(3f / 4f), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(if (foto == null) "sem foto" else "…", color = Cinza)
    }
}

@Composable
private fun CinturaFotosTela() {
    val ctx = LocalContext.current
    val repo = remember { Repo.get(ctx) }
    val scope = rememberCoroutineScope()
    val dao = repo.db.progresso()
    val cinturas by remember { dao.observarCintura() }.collectAsState(initial = emptyList())
    val fotos by remember { dao.observarFotos() }.collectAsState(initial = emptyList())
    var medindo by remember { mutableStateOf<Cintura?>(null) }
    var arquivoPendente by remember { mutableStateOf<Pair<File, String>?>(null) }
    val hoje = LocalDate.now()

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val (arquivo, angulo) = arquivoPendente ?: return@rememberLauncherForActivityResult
        arquivoPendente = null
        if (ok && arquivo.length() > 0) {
            scope.launch { dao.inserirFoto(Foto(data = hoje.toString(), angulo = angulo, arquivo = arquivo.absolutePath)) }
        } else arquivo.delete()
    }
    fun tirar(angulo: String) {
        val pasta = File(ctx.filesDir, "fotos").apply { mkdirs() }
        val arquivo = File(pasta, "${hoje}_${angulo}_${System.currentTimeMillis()}.jpg")
        val uri: Uri = FileProvider.getUriForFile(ctx, "br.com.meushape.arquivos", arquivo)
        arquivoPendente = arquivo to angulo
        try { camera.launch(uri) } catch (_: Exception) { arquivoPendente = null }
    }

    val datasFotos = fotos.map { it.data }.distinct().sorted()
    val ultimaFoto = datasFotos.lastOrNull()?.let { LocalDate.parse(it) }
    val diasDesdeFoto = ultimaFoto?.let { ChronoUnit.DAYS.between(it, hoje) }
    var antes by remember { mutableStateOf<String?>(null) }
    var depois by remember { mutableStateOf<String?>(null) }
    val dAntes = antes ?: datasFotos.firstOrNull()
    val dDepois = depois ?: datasFotos.lastOrNull()

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Spacer(Modifier.height(12.dp))
            CartaoApp {
                Text("📏 Cintura (semanal)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                val ultima = cinturas.lastOrNull()
                val primeira = cinturas.firstOrNull()
                if (ultima != null) {
                    Text("${num(ultima.cm)} cm em ${LocalDate.parse(ultima.data).format(FMT_DM)}", fontSize = 22.sp, color = Limao)
                    if (primeira != null && primeira != ultima) Text("Desde o início: ${num(ultima.cm - primeira.cm)} cm", color = Cinza)
                    val dias = ChronoUnit.DAYS.between(LocalDate.parse(ultima.data), hoje)
                    if (dias >= 7) Text("Já faz $dias dias: hora de medir de novo.", color = Laranja)
                } else Text("Meça na altura do umbigo, de manhã, em jejum.", color = Cinza)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { medindo = Cintura(hoje.toString(), ultima?.cm ?: 85.0) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Registrar cintura") }
                if (cinturas.size >= 2) {
                    Spacer(Modifier.height(8.dp))
                    GraficoLinha(cinturas.map { LocalDate.parse(it.data).format(FMT_DM) to it.cm }, unidade = " cm", cor = Azul)
                }
            }
        }
        item {
            CartaoApp(cor = if (diasDesdeFoto == null || diasDesdeFoto >= 14) Laranja.copy(alpha = 0.18f) else br.com.meushape.ui.theme.Cartao) {
                Text("📸 Fotos (a cada 2 semanas)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    when {
                        diasDesdeFoto == null -> "Tire as primeiras fotos: frente, lado e costas."
                        diasDesdeFoto >= 14 -> "Última sessão há $diasDesdeFoto dias. Hora de novas fotos!"
                        else -> "Próximas fotos em ${14 - diasDesdeFoto} dias."
                    },
                    color = Cinza,
                )
                Text("Mesmo lugar, mesma luz, mesma roupa. As fotos ficam só no celular.", color = Cinza,
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Angulo.TODOS.forEach { (a, nome) ->
                        val jaHoje = fotos.any { it.data == hoje.toString() && it.angulo == a }
                        Button(onClick = { tirar(a) }, modifier = Modifier.weight(1f).height(52.dp)) {
                            Text(if (jaHoje) "$nome ✓" else nome)
                        }
                    }
                }
            }
        }
        if (datasFotos.isNotEmpty()) {
            item {
                Text("Comparação", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text("Antes", color = Cinza)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    datasFotos.forEach { d -> FilterChip(dAntes == d, { antes = d }, { Text(LocalDate.parse(d).format(FMT_DM)) }) }
                }
                Text("Depois", color = Cinza)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    datasFotos.forEach { d -> FilterChip(dDepois == d, { depois = d }, { Text(LocalDate.parse(d).format(FMT_DM)) }) }
                }
            }
            items(Angulo.TODOS, key = { it.first }) { (a, nome) ->
                CartaoApp {
                    Text(nome, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            FotoView(fotos.lastOrNull { it.data == dAntes && it.angulo == a }, Modifier.fillMaxWidth())
                            Text(dAntes?.let { LocalDate.parse(it).format(FMT_DM) } ?: "", color = Cinza)
                        }
                        Column(Modifier.weight(1f)) {
                            FotoView(fotos.firstOrNull { it.data == dDepois && it.angulo == a }, Modifier.fillMaxWidth())
                            Text(dDepois?.let { LocalDate.parse(it).format(FMT_DM) } ?: "", color = Cinza)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    medindo?.let { c ->
        NumeroDialog(
            titulo = "Cintura de ${LocalDate.parse(c.data).format(FMT_DM)}", unidade = "cm", inicial = c.cm,
            onDismiss = { medindo = null },
            onSalvar = { v -> medindo = null; scope.launch { dao.salvarCintura(c.copy(cm = v)) } },
            onApagar = if (cinturas.any { it.data == c.data }) {
                { medindo = null; scope.launch { dao.apagarCintura(c) } }
            } else null,
        )
    }
}

// ---------------- Resumo semanal ----------------

@Composable
private fun ResumoTela() {
    val ctx = LocalContext.current
    val repo = remember { Repo.get(ctx) }
    var segunda by remember { mutableStateOf(ProgressoCalc.segunda(LocalDate.now())) }
    var resumo by remember { mutableStateOf<Repo.Resumo?>(null) }
    LaunchedEffect(segunda) { resumo = withContext(Dispatchers.IO) { repo.resumoSemana(segunda) } }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { segunda = segunda.minusWeeks(1) }) { Text("‹") }
            Text(
                "${segunda.format(FMT_DM)} a ${segunda.plusDays(6).format(FMT_DM)}",
                Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            OutlinedButton(
                onClick = { segunda = segunda.plusWeeks(1) },
                enabled = segunda.isBefore(ProgressoCalc.segunda(LocalDate.now())),
            ) { Text("›") }
        }
        val r = resumo
        if (r == null) Text("Calculando…", color = Cinza) else {
            CartaoApp {
                LinhaResumo("⚖️ Peso médio", r.pesoMedio?.let { "${num(it)} kg" } ?: "sem registros")
                LinhaResumo("🏋 Treinos e caminhadas", "${r.treinos}")
                LinhaResumo("🍽 Refeições cumpridas", "${r.refeicoesFeitas} de ${r.refeicoesPlanejadas}")
                LinhaResumo("😴 Média de sono", horas(r.sonoMedioMin))
                LinhaResumo("👟 Média de passos", "${r.passosMedios}")
                LinhaResumo("🛒 Gasto com compras", reais(r.gastoCompras))
            }
            Text("Todo domingo às 20:00 este resumo chega como notificação.", color = Cinza)
        }
    }
}

@Composable
private fun LinhaResumo(titulo: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(titulo, Modifier.weight(1f))
        Text(valor, fontWeight = FontWeight.Bold)
    }
}

// ---------------- Perfil e backup ----------------

private val CAMPOS_PERFIL = listOf(
    "idade" to "Idade (anos)",
    "altura_cm" to "Altura (cm)",
    "peso_inicial" to "Peso inicial (kg)",
    "data_inicial" to "Data do peso inicial (AAAA-MM-DD)",
    "meta_peso_min" to "Meta mínima (kg)",
    "meta_peso_max" to "Meta máxima (kg)",
    "meta_data" to "Data da meta (AAAA-MM-DD)",
    "kcal" to "Calorias por dia",
    "proteina" to "Proteína (g)",
    "carbo" to "Carboidrato (g)",
    "gordura" to "Gordura (g)",
)

@Composable
private fun PerfilBackupTela() {
    val ctx = LocalContext.current
    val repo = remember { Repo.get(ctx) }
    val scope = rememberCoroutineScope()
    val cfg = perfil(repo)
    var editando by remember { mutableStateOf<Pair<String, String>?>(null) }
    var mensagem by remember { mutableStateOf<String?>(null) }
    var importarUri by remember { mutableStateOf<Uri?>(null) }

    val exportar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            mensagem = try {
                withContext(Dispatchers.IO) { Backup.exportar(ctx, uri) }; "Backup salvo com sucesso."
            } catch (e: Exception) { "Erro ao salvar: ${e.message}" }
        }
    }
    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> importarUri = uri }

    val peso = cfg["peso_inicial"]?.toDoubleOrNull()
    val altura = cfg["altura_cm"]?.toDoubleOrNull()

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Spacer(Modifier.height(12.dp))
            CartaoApp {
                Text("Perfil", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("${cfg["sexo"] ?: "Homem"}, ${cfg["idade"] ?: "-"} anos, ${altura?.let { num(it / 100, 2) } ?: "-"} m")
                if (peso != null && altura != null) Text("IMC inicial: ${num(peso / ((altura / 100) * (altura / 100)))}", color = Cinza)
                Text("Dieta: ${cfg["kcal"]} kcal · P ${cfg["proteina"]} g · C ${cfg["carbo"]} g · G ${cfg["gordura"]} g", color = Limao)
            }
        }
        items(CAMPOS_PERFIL) { (chave, rotulo) ->
            Row(
                Modifier.fillMaxWidth().clickable { editando = chave to (cfg[chave] ?: "") }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(rotulo, Modifier.weight(1f))
                Text(cfg[chave] ?: "-", fontWeight = FontWeight.SemiBold)
                Text("  ✎", color = Cinza)
            }
            HorizontalDivider()
        }
        item {
            Spacer(Modifier.height(12.dp))
            CartaoApp {
                Text("💾 Backup", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Salva todos os dados num arquivo JSON (as fotos não entram e ficam só no celular).", color = Cinza)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { exportar.launch("meu-shape-backup-${LocalDate.now()}.json") },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Exportar backup") }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { importar.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Importar backup") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    editando?.let { (chave, valor) ->
        var texto by remember(chave) { mutableStateOf(valor) }
        val ehData = chave.contains("data")
        val valido = if (ehData) runCatching { LocalDate.parse(texto.trim()) }.isSuccess else lerDecimal(texto) != null
        AlertDialog(
            onDismissRequest = { editando = null },
            title = { Text(CAMPOS_PERFIL.first { it.first == chave }.second) },
            text = {
                OutlinedTextField(
                    texto, { texto = it }, singleLine = true, isError = !valido,
                    keyboardOptions = KeyboardOptions(keyboardType = if (ehData) KeyboardType.Text else KeyboardType.Decimal),
                )
            },
            confirmButton = {
                TextButton(enabled = valido, onClick = {
                    val v = if (ehData) texto.trim() else lerDecimal(texto)!!.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
                    editando = null
                    scope.launch { repo.db.config().salvar(Config(chave, v)) }
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { editando = null }) { Text("Cancelar") } },
        )
    }
    importarUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { importarUri = null },
            title = { Text("Importar backup?") },
            text = { Text("Todos os dados atuais serão substituídos pelos do arquivo. As fotos não são afetadas.") },
            confirmButton = {
                TextButton(onClick = {
                    importarUri = null
                    scope.launch {
                        mensagem = try {
                            val n = withContext(Dispatchers.IO) { Backup.importar(ctx, uri) }
                            withContext(Dispatchers.IO) { Alarmes.agendarProximo(ctx); ResumoSemanal.agendar(ctx) }
                            "Backup importado: $n registros."
                        } catch (e: Exception) { "Não foi possível importar: ${e.message}" }
                    }
                }) { Text("Importar", color = Vermelho) }
            },
            dismissButton = { TextButton(onClick = { importarUri = null }) { Text("Cancelar") } },
        )
    }
    mensagem?.let { m ->
        AlertDialog(
            onDismissRequest = { mensagem = null },
            text = { Text(m) },
            confirmButton = { TextButton(onClick = { mensagem = null }) { Text("OK") } },
        )
    }
}
