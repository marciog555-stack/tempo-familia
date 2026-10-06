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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.meushape.data.ItemPlano
import br.com.meushape.data.Marmita
import br.com.meushape.data.Repo
import br.com.meushape.data.TipoItem
import br.com.meushape.logic.Horario
import br.com.meushape.logic.TipoDia
import br.com.meushape.notify.Alarmes
import br.com.meushape.ui.CartaoApp
import br.com.meushape.ui.EscolherHora
import br.com.meushape.ui.theme.Cinza
import br.com.meushape.ui.theme.Vermelho
import kotlinx.coroutines.launch

/** Edição da rotina (horários e alimentos) de plantão e de folga. */
@Composable
fun EditarRotinaScreen(onVoltar: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { Repo.get(ctx) }
    val scope = rememberCoroutineScope()
    var aba by remember { mutableIntStateOf(0) }
    val tipo = if (aba == 0) TipoDia.PLANTAO else TipoDia.FOLGA
    val itens by remember(tipo) { repo.observarPlano(tipo) }.collectAsState(initial = emptyList())
    var editando by remember { mutableStateOf<ItemPlano?>(null) }

    Column(Modifier.fillMaxSize()) {
        TextButton(onClick = onVoltar, modifier = Modifier.padding(8.dp)) { Text("← Voltar") }
        TabRow(selectedTabIndex = aba) {
            Tab(selected = aba == 0, onClick = { aba = 0 }, text = { Text("Plantão") })
            Tab(selected = aba == 1, onClick = { aba = 1 }, text = { Text("Folga") })
        }
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            items(itens, key = { it.id }) { item ->
                CartaoApp(Modifier.clickable { editando = item }) {
                    Text(
                        Horario.texto(item.inicioMin) + (item.fimMin?.let { " – " + Horario.texto(it) } ?: ""),
                        color = Cinza,
                    )
                    Text(item.titulo, fontWeight = FontWeight.Bold)
                    if (item.descricao.isNotBlank()) Text(item.descricao, style = MaterialTheme.typography.bodySmall)
                    if (!item.notificar) Text("Sem notificação", color = Cinza, style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                Button(
                    onClick = {
                        editando = ItemPlano(
                            tipoDia = Repo.chavePlano(tipo), inicioMin = 12 * 60,
                            tipo = TipoItem.REFEICAO, titulo = "",
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text("+ Adicionar item") }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    editando?.let { original ->
        EditarItemDialog(
            original = original,
            onDismiss = { editando = null },
            onSalvar = { novo ->
                editando = null
                scope.launch { repo.db.plano().salvar(novo); Alarmes.agendarProximo(ctx) }
            },
            onApagar = if (original.id == 0L) null else {
                {
                    editando = null
                    scope.launch { repo.db.plano().apagar(original); Alarmes.agendarProximo(ctx) }
                }
            },
        )
    }
}

@Composable
private fun EditarItemDialog(
    original: ItemPlano,
    onDismiss: () -> Unit,
    onSalvar: (ItemPlano) -> Unit,
    onApagar: (() -> Unit)?,
) {
    var titulo by remember { mutableStateOf(original.titulo) }
    var descricao by remember { mutableStateOf(original.descricao) }
    var opcoes by remember { mutableStateOf(original.opcoes.replace("|", "\n")) }
    var tituloAlt by remember { mutableStateOf(original.tituloAlt) }
    var descricaoAlt by remember { mutableStateOf(original.descricaoAlt) }
    var inicio by remember { mutableIntStateOf(original.inicioMin) }
    var fim by remember { mutableStateOf(original.fimMin) }
    var tipo by remember { mutableStateOf(original.tipo) }
    var marmita by remember { mutableStateOf(original.marmita) }
    var notificar by remember { mutableStateOf(original.notificar) }
    var escolhendo by remember { mutableStateOf<String?>(null) } // "inicio" ou "fim"
    var confirmarApagar by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (original.id == 0L) "Novo item" else "Editar item") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(titulo, { titulo = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(descricao, { descricao = it }, label = { Text("Descrição / alimentos") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { escolhendo = "inicio" }, modifier = Modifier.weight(1f)) {
                        Text("Início ${Horario.texto(inicio)}")
                    }
                    OutlinedButton(onClick = { escolhendo = "fim" }, modifier = Modifier.weight(1f)) {
                        Text(fim?.let { "Fim ${Horario.texto(it)}" } ?: "Sem fim")
                    }
                }
                if (fim != null) TextButton(onClick = { fim = null }) { Text("Remover horário de fim") }
                Text("Tipo", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(tipo == TipoItem.REFEICAO, { tipo = TipoItem.REFEICAO }, { Text("Refeição") })
                    FilterChip(tipo == TipoItem.TREINO, { tipo = TipoItem.TREINO }, { Text("Treino") })
                    FilterChip(tipo == TipoItem.DESCANSO, { tipo = TipoItem.DESCANSO }, { Text("Sono") })
                }
                if (original.tipo == TipoItem.TREINO_FOLGA) {
                    FilterChip(tipo == TipoItem.TREINO_FOLGA, { tipo = TipoItem.TREINO_FOLGA }, { Text("Treino ou caminhada (alterna)") })
                }
                if (tipo == TipoItem.TREINO_FOLGA) {
                    OutlinedTextField(tituloAlt, { tituloAlt = it }, label = { Text("Nome no dia de caminhada") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(descricaoAlt, { descricaoAlt = it }, label = { Text("Descrição no dia de caminhada") }, modifier = Modifier.fillMaxWidth())
                }
                if (tipo == TipoItem.REFEICAO) {
                    OutlinedTextField(
                        opcoes, { opcoes = it },
                        label = { Text("Opções para escolher (uma por linha, opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Usa marmita do estoque?", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(marmita == Marmita.NENHUMA, { marmita = Marmita.NENHUMA }, { Text("Não") })
                        FilterChip(marmita == Marmita.ALMOCO, { marmita = Marmita.ALMOCO }, { Text("Almoço") })
                        FilterChip(marmita == Marmita.JANTA, { marmita = Marmita.JANTA }, { Text("Janta") })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Notificar no horário", Modifier.weight(1f))
                    Switch(notificar, { notificar = it })
                }
                if (onApagar != null) {
                    TextButton(onClick = { confirmarApagar = true }) { Text("Apagar item", color = Vermelho) }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = titulo.isNotBlank(),
                onClick = {
                    onSalvar(
                        original.copy(
                            titulo = titulo.trim(), descricao = descricao.trim(),
                            opcoes = opcoes.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("|"),
                            tituloAlt = tituloAlt.trim(), descricaoAlt = descricaoAlt.trim(),
                            inicioMin = inicio, fimMin = fim, tipo = tipo,
                            marmita = if (tipo == TipoItem.REFEICAO) marmita else Marmita.NENHUMA,
                            notificar = notificar,
                        )
                    )
                },
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )

    escolhendo?.let { qual ->
        val atual = if (qual == "inicio") inicio else (fim ?: (inicio + 60))
        val m = Math.floorMod(atual, 1440)
        EscolherHora(m / 60, m % 60, onDismiss = { escolhendo = null }) { h, mi ->
            val v = Horario.deTexto(h, mi)
            if (qual == "inicio") inicio = v else fim = v
            escolhendo = null
        }
    }
    if (confirmarApagar && onApagar != null) {
        AlertDialog(
            onDismissRequest = { confirmarApagar = false },
            title = { Text("Apagar \"${original.titulo}\"?") },
            confirmButton = { TextButton(onClick = { confirmarApagar = false; onApagar() }) { Text("Apagar", color = Vermelho) } },
            dismissButton = { TextButton(onClick = { confirmarApagar = false }) { Text("Cancelar") } },
        )
    }
}
