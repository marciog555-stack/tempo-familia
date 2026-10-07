package br.com.tempofamilia.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import br.com.tempofamilia.data.AppState
import br.com.tempofamilia.data.Password
import br.com.tempofamilia.data.Store
import br.com.tempofamilia.ui.SectionTitle
import br.com.tempofamilia.ui.StatusLine
import br.com.tempofamilia.ui.rememberPasswordGate
import br.com.tempofamilia.ui.rememberResumeTick
import br.com.tempofamilia.util.Perms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Ajustes: proteções, liberação temporária, senha e permissões. */
@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val estado by Store.state.collectAsState()
    val gate = rememberPasswordGate()
    val tick = rememberResumeTick()
    var trocandoSenha by remember { mutableStateOf(false) }
    var agora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { agora = System.currentTimeMillis(); delay(1000) }
    }
    val liberadoSeg = ((estado.unlockedUntil - agora) / 1000).coerceAtLeast(0)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        SectionTitle("Proteções")
        Text("Ligar é sempre livre. Desligar pede a senha.", style = MaterialTheme.typography.bodySmall)
        Protecao("Limites de apps e horários", estado.limitsEnabled, gate) { v -> { s: AppState -> s.copy(limitsEnabled = v) } }
        Protecao("Bloqueio de palavras e sites", estado.wordBlockEnabled, gate) { v -> { s: AppState -> s.copy(wordBlockEnabled = v) } }
        Protecao("Proteção das configurações", estado.settingsGuardEnabled, gate) { v -> { s: AppState -> s.copy(settingsGuardEnabled = v) } }

        SectionTitle("Liberação semanal de sites")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Liberar 15 minutos por semana", modifier = Modifier.weight(1f))
                    Switch(checked = estado.weeklyReleaseEnabled, onCheckedChange = { novo ->
                        if (novo) {
                            // Afrouxar a proteção: exige a senha da pessoa de confiança.
                            gate.ask("Ativar a liberação semanal: uma vez por semana, o bloqueio de sites fica desligado por 15 minutos.") {
                                Store.update { it.copy(weeklyReleaseEnabled = true, weeklyReleaseActivatedAt = System.currentTimeMillis()) }
                            }
                        } else {
                            // Deixar mais rígido é sempre livre.
                            Store.update { it.copy(weeklyReleaseEnabled = false, weeklyReleaseStartedAt = 0L) }
                        }
                    })
                }
                Text(
                    "Uma vez por semana (segunda a domingo), o bloqueio de palavras e sites fica desligado por " +
                        "15 minutos e depois volta sozinho. Os limites de apps e as telas protegidas continuam valendo.\n" +
                        "Ativar pede a senha. Desligar é livre, a qualquer momento. Quando estiver pronto, desligue de vez.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        SectionTitle("Liberação temporária")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (liberadoSeg > 0) {
                    Text("Configurações liberadas por mais %d:%02d.".format(liberadoSeg / 60, liberadoSeg % 60))
                    Button(onClick = { Store.update { it.copy(unlockedUntil = 0L) } }) { Text("Proteger agora") }
                } else {
                    Text(
                        "Para mexer nas telas protegidas do Android (Acessibilidade, página do app, " +
                            "administradores, DNS privado), a pessoa de confiança libera por 10 minutos.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(onClick = {
                        gate.ask("Liberar as configurações protegidas por 10 minutos.") {
                            Store.update { it.copy(unlockedUntil = System.currentTimeMillis() + 10 * 60_000L) }
                        }
                    }) { Text("Liberar por 10 minutos") }
                }
            }
        }

        SectionTitle("Permissões")
        val perms = remember(tick) {
            listOf(
                Triple("Acesso ao uso", Perms.usage(ctx), Perms.usageIntent(ctx)),
                Triple("Sobreposição a outros apps", Perms.overlay(ctx), Perms.overlayIntent(ctx)),
                Triple("Acessibilidade", Perms.accessibility(ctx), Perms.accessibilityIntent()),
                Triple("Administrador do dispositivo", Perms.admin(ctx), Perms.adminIntent(ctx)),
                Triple("Sem otimização de bateria", Perms.battery(ctx), Perms.batteryIntent(ctx)),
                Triple("Página do app (configurações restritas)", true, Perms.appDetailsIntent(ctx)),
            )
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                perms.forEachIndexed { i, (nome, ok, intent) ->
                    PermissaoLinha(nome, ok, intent)
                    if (i < perms.lastIndex) HorizontalDivider()
                }
            }
        }
        Text(
            "Se uma tela estiver protegida, use \"Liberar por 10 minutos\" antes.",
            style = MaterialTheme.typography.bodySmall,
        )

        SectionTitle("Senha")
        OutlinedButton(onClick = { gate.ask("Confirme a senha atual para trocá-la.") { trocandoSenha = true } }) {
            Text("Trocar a senha")
        }

        SectionTitle("Privacidade")
        Text(
            "O Tempo Família não tem permissão de internet: nada sai do celular. Sem anúncios e sem análises.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text("Versão ${versao(ctx)}", style = MaterialTheme.typography.bodySmall)
    }

    if (trocandoSenha) TrocarSenhaDialog { trocandoSenha = false }
}

private fun versao(ctx: android.content.Context): String =
    runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "?"

@Composable
private fun Protecao(
    nome: String,
    ligado: Boolean,
    gate: br.com.tempofamilia.ui.PasswordGate,
    mudanca: (Boolean) -> (AppState) -> AppState,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(nome, modifier = Modifier.weight(1f))
        Switch(checked = ligado, onCheckedChange = { novo ->
            if (novo) Store.update(mudanca(true))
            else gate.ask("Desligar \"$nome\".") { Store.update(mudanca(false)) }
        })
    }
}

@Composable
private fun PermissaoLinha(nome: String, ok: Boolean, intent: Intent) {
    val ctx = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { StatusLine(ok, nome) }
        TextButton(onClick = { Perms.open(ctx, intent) }) { Text("Abrir") }
    }
}

@Composable
private fun TrocarSenhaDialog(onFechar: () -> Unit) {
    var senha by remember { mutableStateOf("") }
    var repetir by remember { mutableStateOf("") }
    var salvando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val valido = senha.length >= 6 && senha == repetir
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Nova senha") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = senha, onValueChange = { senha = it }, label = { Text("Nova senha (mín. 6)") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                OutlinedTextField(
                    value = repetir, onValueChange = { repetir = it }, label = { Text("Repita a senha") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = repetir.isNotEmpty() && repetir != senha,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    salvando = true
                    scope.launch {
                        withContext(Dispatchers.Default) { Password.setNew(senha) }
                        onFechar()
                    }
                },
                enabled = valido && !salvando,
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}
