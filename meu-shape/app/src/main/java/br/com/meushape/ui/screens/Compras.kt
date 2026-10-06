package br.com.meushape.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.meushape.data.CompraMarcada
import br.com.meushape.data.EstoqueMarmita
import br.com.meushape.data.ItemCompra
import br.com.meushape.data.Lista
import br.com.meushape.data.Marmita
import br.com.meushape.data.Repo
import br.com.meushape.logic.Escala
import br.com.meushape.ui.CartaoApp
import br.com.meushape.ui.theme.Cartao
import br.com.meushape.ui.theme.Cinza
import br.com.meushape.ui.theme.Laranja
import br.com.meushape.ui.theme.Limao
import br.com.meushape.ui.theme.Vermelho
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDateTime
import java.util.Locale

private val REAIS: NumberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
fun reais(v: Double): String = REAIS.format(v)

/** Quantidade e preço do item conforme a versão (completa/econômica). */
private fun ItemCompra.qtd(eco: Boolean) = if (eco && quantidadeEco.isNotBlank()) quantidadeEco else quantidade
private fun ItemCompra.valor(eco: Boolean) = if (eco) (precoEco ?: preco) else preco
private fun ItemCompra.visivel(eco: Boolean) = if (eco) noEconomico else noCompleto

@Composable
fun ComprasAba() {
    var aba by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = aba) {
            Tab(aba == 0, { aba = 0 }, text = { Text("Semanal") })
            Tab(aba == 1, { aba = 1 }, text = { Text("Mensal") })
            Tab(aba == 2, { aba = 2 }, text = { Text("Marmitas") })
        }
        when (aba) {
            0 -> ListaCompras(Lista.SEMANAL)
            1 -> ListaCompras(Lista.MENSAL)
            else -> MarmitasScreen()
        }
    }
}

@Composable
private fun ListaCompras(lista: String) {
    val ctx = LocalContext.current
    val repo = remember { Repo.get(ctx) }
    val scope = rememberCoroutineScope()
    val hoje = Escala.diaLogico(LocalDateTime.now())
    val periodoSemana = Repo.periodo(Lista.SEMANAL, hoje)
    val periodoMes = Repo.periodo(Lista.MENSAL, hoje)
    val periodo = if (lista == Lista.SEMANAL) periodoSemana else periodoMes

    val todos by remember { repo.db.compras().observarItens() }.collectAsState(initial = emptyList())
    val marcadas by remember(periodoSemana, periodoMes) {
        repo.db.compras().observarMarcadas(listOf(periodoSemana, periodoMes))
    }.collectAsState(initial = emptyList())
    val eco by remember {
        repo.db.config().observar().map { l -> l.firstOrNull { it.chave == "compras_economico" }?.valor == "1" }
    }.collectAsState(initial = false)
    var editando by remember { mutableStateOf<ItemCompra?>(null) }

    val itens = todos.filter { it.lista == lista && it.visivel(eco) }
    val marcadasAqui = marcadas.filter { it.periodo == periodo }.map { it.itemId }.toSet()
    val totalSemana = todos.filter { it.lista == Lista.SEMANAL && it.visivel(eco) }.sumOf { it.valor(eco) }
    val totalMensal = todos.filter { it.lista == Lista.MENSAL && it.visivel(eco) }.sumOf { it.valor(eco) }
    val totalLista = itens.sumOf { it.valor(eco) }
    val gastoMarcado = itens.filter { it.id in marcadasAqui }.sumOf { it.valor(eco) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !eco, onClick = { scope.launch { repo.definirModoEconomico(false) } },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("Completa") }
                SegmentedButton(
                    selected = eco, onClick = { scope.launch { repo.definirModoEconomico(true) } },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("Econômica") }
            }
        }
        item {
            CartaoApp {
                Text(
                    if (lista == Lista.SEMANAL) "Lista da semana (zera toda segunda)" else "Lista do mês (zera todo dia 1º)",
                    color = Cinza,
                )
                Text("Total desta lista: ${reais(totalLista)}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Já comprado: ${reais(gastoMarcado)} · ${marcadasAqui.size} de ${itens.size} itens", color = Limao)
                Spacer(Modifier.height(6.dp))
                Text("Semana: ${reais(totalSemana)}")
                Text("Mês: ${reais(totalMensal + totalSemana * 4.33)} (mensal + 4,33 semanas)", color = Cinza)
            }
        }
        items(itens, key = { it.id }) { item ->
            val marcado = item.id in marcadasAqui
            CartaoApp(Modifier.clickable {
                scope.launch {
                    if (marcado) repo.db.compras().desmarcar(item.id, periodo)
                    else repo.db.compras().marcar(CompraMarcada(item.id, periodo, item.valor(eco)))
                }
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = marcado, onCheckedChange = null, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.nome, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                            textDecoration = if (marcado) TextDecoration.LineThrough else null,
                            color = if (marcado) Cinza else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            listOf(item.qtd(eco), if (item.valor(eco) > 0) reais(item.valor(eco)) else "sem preço")
                                .filter { it.isNotBlank() }.joinToString(" · "),
                            color = Cinza,
                        )
                    }
                    IconButton(onClick = { editando = item }) { Icon(Icons.Filled.Edit, "Editar") }
                }
            }
        }
        item {
            Button(
                onClick = { editando = ItemCompra(lista = lista, nome = "", ordem = (todos.maxOfOrNull { it.ordem } ?: 0) + 1) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("+ Adicionar item") }
            Spacer(Modifier.height(24.dp))
        }
    }

    editando?.let { original ->
        EditarCompraDialog(
            original = original,
            onDismiss = { editando = null },
            onSalvar = { novo -> editando = null; scope.launch { repo.db.compras().salvar(novo) } },
            onApagar = if (original.id == 0L) null else {
                { editando = null; scope.launch { repo.db.compras().apagar(original) } }
            },
        )
    }
}

private fun lerPreco(t: String): Double? =
    t.trim().replace("R$", "").replace(" ", "").replace(".", "").replace(",", ".").toDoubleOrNull()

private fun precoTexto(v: Double?): String =
    if (v == null || v == 0.0) "" else "%.2f".format(Locale("pt", "BR"), v)

@Composable
private fun EditarCompraDialog(
    original: ItemCompra,
    onDismiss: () -> Unit,
    onSalvar: (ItemCompra) -> Unit,
    onApagar: (() -> Unit)?,
) {
    var nome by remember { mutableStateOf(original.nome) }
    var qtd by remember { mutableStateOf(original.quantidade) }
    var preco by remember { mutableStateOf(precoTexto(original.preco)) }
    var noCompleto by remember { mutableStateOf(original.noCompleto) }
    var noEco by remember { mutableStateOf(original.noEconomico) }
    var qtdEco by remember { mutableStateOf(original.quantidadeEco) }
    var precoEco by remember { mutableStateOf(precoTexto(original.precoEco)) }
    var lista by remember { mutableStateOf(original.lista) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (original.id == 0L) "Novo item" else "Editar item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nome, { nome = it }, label = { Text("Nome") }, singleLine = true)
                OutlinedTextField(qtd, { qtd = it }, label = { Text("Quantidade") }, singleLine = true)
                OutlinedTextField(
                    preco, { preco = it }, label = { Text("Preço total (R$)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(lista == Lista.SEMANAL, { lista = Lista.SEMANAL }, { Text("Semanal") })
                    FilterChip(lista == Lista.MENSAL, { lista = Lista.MENSAL }, { Text("Mensal") })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Na versão completa", Modifier.weight(1f)); Switch(noCompleto, { noCompleto = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Na versão econômica", Modifier.weight(1f)); Switch(noEco, { noEco = it })
                }
                if (noEco) {
                    OutlinedTextField(qtdEco, { qtdEco = it }, label = { Text("Quantidade na econômica (opcional)") }, singleLine = true)
                    OutlinedTextField(
                        precoEco, { precoEco = it }, label = { Text("Preço na econômica (opcional)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
                if (onApagar != null) TextButton(onClick = onApagar) { Text("Apagar item", color = Vermelho) }
            }
        },
        confirmButton = {
            TextButton(enabled = nome.isNotBlank(), onClick = {
                onSalvar(
                    original.copy(
                        nome = nome.trim(), quantidade = qtd.trim(), preco = lerPreco(preco) ?: 0.0,
                        noCompleto = noCompleto, noEconomico = noEco, quantidadeEco = qtdEco.trim(),
                        precoEco = lerPreco(precoEco), lista = lista,
                    )
                )
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

// ---------------- Marmitas ----------------

private const val META_ALMOCO = 7
private const val META_JANTA = 4

@Composable
private fun MarmitasScreen() {
    val ctx = LocalContext.current
    val repo = remember { Repo.get(ctx) }
    val scope = rememberCoroutineScope()
    val hoje = Escala.diaLogico(LocalDateTime.now())
    val seg = Repo.inicioSemana(hoje)
    val estoque by remember { repo.db.marmitas().observarEstoque() }.collectAsState(initial = emptyList())
    val montadas by remember(seg) {
        repo.db.marmitas().observarMontadas(seg.toString(), seg.plusDays(6).toString())
    }.collectAsState(initial = emptyList())
    var montando by remember { mutableStateOf<String?>(null) }
    var ajustando by remember { mutableStateOf<EstoqueMarmita?>(null) }

    val montAlmoco = montadas.filter { it.tipo == Marmita.ALMOCO }.sumOf { it.quantidade }
    val montJanta = montadas.filter { it.tipo == Marmita.JANTA }.sumOf { it.quantidade }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            CartaoApp {
                Text("Meta da semana: 11 marmitas", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Montadas: ${montAlmoco + montJanta} de 11", color = Limao)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { ((montAlmoco + montJanta) / 11f).coerceAtMost(1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text("Almoço: $montAlmoco de $META_ALMOCO · Janta: $montJanta de $META_JANTA", color = Cinza)
            }
        }
        listOf(Marmita.ALMOCO to "Almoço", Marmita.JANTA to "Janta").forEach { (tipo, nome) ->
            item(key = tipo) {
                val e = estoque.firstOrNull { it.tipo == tipo } ?: EstoqueMarmita(tipo)
                val baixo = e.total <= 2
                CartaoApp(cor = if (baixo) Laranja.copy(alpha = 0.18f) else Cartao) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("🍱 $nome", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Geladeira: ${e.geladeira} · Freezer: ${e.freezer}")
                            Text("Total: ${e.total}", color = if (baixo) Laranja else Limao, fontWeight = FontWeight.SemiBold)
                            if (baixo) Text("Estoque baixo! Monte mais.", color = Laranja)
                        }
                        IconButton(onClick = { ajustando = e }) { Icon(Icons.Filled.Edit, "Ajustar") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { montando = tipo }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Montei") }
                        OutlinedButton(
                            onClick = { scope.launch { repo.ajustarEstoque(e.copy(freezer = e.freezer - 1, geladeira = e.geladeira + 1)) } },
                            enabled = e.freezer > 0,
                            modifier = Modifier.weight(1f).height(52.dp),
                        ) { Text("Freezer → geladeira") }
                    }
                }
            }
        }
        item {
            Text(
                "A baixa é automática: ao marcar o almoço (ou a janta da folga) como feito na tela Hoje, " +
                    "sai 1 marmita da geladeira (ou do freezer, se a geladeira estiver vazia). " +
                    "A refeição livre e a janta da empresa não mexem no estoque.",
                color = Cinza, style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    montando?.let { tipo ->
        var qtd by remember { mutableIntStateOf(if (tipo == Marmita.ALMOCO) META_ALMOCO else META_JANTA) }
        var freezer by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { montando = null },
            title = { Text("Montei marmitas de ${if (tipo == Marmita.ALMOCO) "almoço" else "janta"}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Contador(qtd) { qtd = it.coerceIn(1, 30) }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(!freezer, { freezer = false }, { Text("Geladeira") })
                        FilterChip(freezer, { freezer = true }, { Text("Freezer") })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val q = qtd; val f = freezer
                    scope.launch { repo.montarMarmitas(tipo, q, f, hoje) }
                    montando = null
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { montando = null }) { Text("Cancelar") } },
        )
    }
    ajustando?.let { e ->
        var gel by remember { mutableIntStateOf(e.geladeira) }
        var frz by remember { mutableIntStateOf(e.freezer) }
        AlertDialog(
            onDismissRequest = { ajustando = null },
            title = { Text("Ajustar estoque") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Geladeira"); Contador(gel) { gel = it.coerceAtLeast(0) }
                    Text("Freezer"); Contador(frz) { frz = it.coerceAtLeast(0) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val novo = e.copy(geladeira = gel, freezer = frz)
                    scope.launch { repo.ajustarEstoque(novo) }
                    ajustando = null
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { ajustando = null }) { Text("Cancelar") } },
        )
    }
}


@Composable
private fun Contador(valor: Int, onMudar: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { onMudar(valor - 1) }, modifier = Modifier.size(56.dp)) { Text("−", fontSize = 22.sp) }
        Text("$valor", Modifier.width(64.dp), fontSize = 26.sp, fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        OutlinedButton(onClick = { onMudar(valor + 1) }, modifier = Modifier.size(56.dp)) { Text("+", fontSize = 22.sp) }
    }
}
