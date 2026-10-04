package br.com.tempofamilia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.tempofamilia.data.DefaultTerms
import br.com.tempofamilia.data.Store
import br.com.tempofamilia.ui.InfoCard
import br.com.tempofamilia.ui.rememberPasswordGate
import br.com.tempofamilia.util.KeywordMatcher
import br.com.tempofamilia.util.TextNormalizer

/** Lista de termos bloqueados. Adicionar é livre; remover pede a senha. */
@Composable
fun WordsScreen() {
    val estado by Store.state.collectAsState()
    val gate = rememberPasswordGate()
    var novo by remember { mutableStateOf("") }
    var verPadrao by remember { mutableStateOf(false) }
    val normalizado = TextNormalizer.normalize(novo)
    val erro = when {
        novo.isBlank() -> null
        normalizado.length < 3 -> "Use pelo menos 3 letras ou números."
        estado.customTerms.any { TextNormalizer.normalize(it) == normalizado } -> "Este termo já está na lista."
        else -> null
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Bloqueio de palavras", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Se um destes termos for digitado em qualquer app ou aparecer em um navegador ou busca, " +
                        "a página é fechada na hora. Acentos, maiúsculas e espaços são ignorados.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (!estado.wordBlockEnabled) {
            item {
                InfoCard(
                    "Bloqueio de palavras desligado",
                    "Ligue novamente em Ajustes (não precisa de senha).",
                    MaterialTheme.colorScheme.errorContainer,
                )
            }
        }
        item {
            InfoCard(
                "Lista padrão ativa",
                "${KeywordMatcher.totalPadrao} termos e domínios adultos em português e inglês já estão bloqueados.",
                MaterialTheme.colorScheme.primaryContainer,
            ) {
                TextButton(onClick = { verPadrao = !verPadrao }) {
                    Text(if (verPadrao) "Esconder lista" else "Ver lista")
                }
                if (verPadrao) {
                    Text(DefaultTerms.ALL.joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = novo,
                    onValueChange = { novo = it },
                    label = { Text("Novo termo ou site") },
                    singleLine = true,
                    isError = erro != null,
                    supportingText = { erro?.let { Text(it) } },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        val termo = novo.trim()
                        Store.update { it.copy(customTerms = it.customTerms + termo) }
                        novo = ""
                    },
                    enabled = novo.isNotBlank() && erro == null,
                ) { Text("Adicionar") }
            }
        }
        item {
            Text(
                "Seus termos (${estado.customTerms.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        items(estado.customTerms) { termo ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(termo, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        gate.ask("Remover o termo \"$termo\" da lista de bloqueio.") {
                            Store.update { s -> s.copy(customTerms = s.customTerms - termo) }
                        }
                    }) { Icon(Icons.Filled.Delete, contentDescription = "Remover") }
                }
            }
        }
        item { Text(" ") }
    }
}
