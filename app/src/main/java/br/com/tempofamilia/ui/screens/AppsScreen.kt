package br.com.tempofamilia.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import br.com.tempofamilia.data.AppLimit
import br.com.tempofamilia.data.Store
import br.com.tempofamilia.ui.rememberPasswordGate
import br.com.tempofamilia.util.formatMinutos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** App instalado que pode ser escolhido para limitar. */
private class AppInstalado(val pkg: String, val nome: String, val icone: ImageBitmap?)

/** Pacotes populares que aparecem no topo da lista. */
private val POPULARES = listOf(
    "com.instagram.android", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
    "com.google.android.youtube", "com.facebook.katana", "com.twitter.android",
    "com.kwai.video", "com.facebook.lite", "com.snapchat.android", "com.pinterest",
    "com.reddit.frontpage", "com.instagram.barcelona", "com.netflix.mediaclient",
)

private fun listarApps(ctx: Context): List<AppInstalado> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .distinctBy { it.activityInfo.packageName }
        .filter { it.activityInfo.packageName != ctx.packageName }
        .map {
            val icone = runCatching { it.loadIcon(pm).toBitmap(96, 96).asImageBitmap() }.getOrNull()
            AppInstalado(it.activityInfo.packageName, it.loadLabel(pm).toString(), icone)
        }
        .sortedWith(compareBy<AppInstalado> {
            val i = POPULARES.indexOf(it.pkg); if (i >= 0) i else Int.MAX_VALUE
        }.thenBy { it.nome.lowercase() })
}

@Composable
fun AppsScreen() {
    val estado by Store.state.collectAsState()
    val gate = rememberPasswordGate()
    var escolhendo by remember { mutableStateOf(false) }
    var novoApp by remember { mutableStateOf<AppInstalado?>(null) }
    var editando by remember { mutableStateOf<AppLimit?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { escolhendo = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Adicionar app") },
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(Modifier.padding(top = 16.dp)) {
                    Text("Apps limitados", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Toque num app para mudar o limite. Diminuir é livre; aumentar ou remover pede a senha.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (estado.limits.isEmpty()) {
                item { Text("Nenhum app ainda. Toque em \"Adicionar app\".", modifier = Modifier.padding(vertical = 24.dp)) }
            }
            items(estado.limits, key = { it.packageName }) { lim ->
                Card(Modifier.fillMaxWidth().clickable { editando = lim }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(lim.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (lim.minutesPerDay == 0) "Bloqueado o dia todo"
                                else "Limite: ${formatMinutos(lim.minutesPerDay.toLong())} por dia",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        IconButton(onClick = {
                            gate.ask("Remover o limite do ${lim.label}.") {
                                Store.update { s -> s.copy(limits = s.limits.filterNot { it.packageName == lim.packageName }) }
                            }
                        }) { Icon(Icons.Filled.Delete, contentDescription = "Remover") }
                    }
                }
            }
            item { Spacer(Modifier.padding(40.dp)) }
        }
    }

    if (escolhendo) {
        EscolherAppDialog(
            jaLimitados = estado.limits.map { it.packageName }.toSet(),
            onDismiss = { escolhendo = false },
            onEscolhido = { escolhendo = false; novoApp = it },
        )
    }
    novoApp?.let { app ->
        LimiteDialog(
            titulo = app.nome,
            inicial = 30,
            onDismiss = { novoApp = null },
            onSalvar = { min ->
                novoApp = null
                // Adicionar um app torna as regras mais rígidas: não pede senha.
                Store.update { s ->
                    s.copy(limits = s.limits.filterNot { it.packageName == app.pkg } + AppLimit(app.pkg, app.nome, min))
                }
            },
        )
    }
    editando?.let { lim ->
        LimiteDialog(
            titulo = lim.label,
            inicial = lim.minutesPerDay,
            onDismiss = { editando = null },
            onSalvar = { min ->
                editando = null
                val salvar = {
                    Store.update { s ->
                        s.copy(limits = s.limits.map { if (it.packageName == lim.packageName) it.copy(minutesPerDay = min) else it })
                    }
                }
                if (min > lim.minutesPerDay) gate.ask("Aumentar o limite do ${lim.label} para ${formatMinutos(min.toLong())}.", salvar)
                else salvar()
            },
        )
    }
}

@Composable
private fun EscolherAppDialog(jaLimitados: Set<String>, onDismiss: () -> Unit, onEscolhido: (AppInstalado) -> Unit) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<AppInstalado>?>(null) }
    var busca by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) { listarApps(ctx) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escolha um app") },
        text = {
            Column {
                OutlinedTextField(
                    value = busca, onValueChange = { busca = it },
                    label = { Text("Buscar") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(Modifier.heightIn(min = 200.dp, max = 420.dp)) {
                    val lista = apps
                    if (lista == null) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    } else {
                        val filtrada = lista.filter {
                            it.pkg !in jaLimitados && (busca.isBlank() || it.nome.contains(busca, ignoreCase = true))
                        }
                        LazyColumn {
                            items(filtrada, key = { it.pkg }) { app ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { onEscolhido(app) }.padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    app.icone?.let { Image(it, null, Modifier.size(36.dp)) }
                                    Spacer(Modifier.width(12.dp))
                                    Text(app.nome)
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun LimiteDialog(titulo: String, inicial: Int, onDismiss: () -> Unit, onSalvar: (Int) -> Unit) {
    var texto by remember { mutableStateOf(inicial.toString()) }
    val valor = texto.toIntOrNull()
    val valido = valor != null && valor in 0..1440
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Limite diário em minutos (0 = bloqueado o dia todo):")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(15, 30, 60, 120).forEach { m ->
                        FilterChip(selected = valor == m, onClick = { texto = m.toString() }, label = { Text(formatMinutos(m.toLong())) })
                    }
                }
                OutlinedTextField(
                    value = texto,
                    onValueChange = { novo -> texto = novo.filter { it.isDigit() }.take(4) },
                    label = { Text("Minutos por dia") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !valido,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSalvar(valor!!) }, enabled = valido) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
