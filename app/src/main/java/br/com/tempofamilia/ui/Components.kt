package br.com.tempofamilia.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import br.com.tempofamilia.data.Password
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Caixa que pede a senha da pessoa de confiança. */
@Composable
fun PasswordDialog(motivo: String, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var senha by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    var conferindo by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun conferir() {
        if (conferindo) return
        conferindo = true
        scope.launch {
            val ok = withContext(Dispatchers.Default) { Password.verify(senha) }
            conferindo = false
            if (ok) {
                onSuccess()
            } else {
                val espera = Password.segundosDeEspera()
                erro = if (espera > 0) "Muitas tentativas. Aguarde $espera segundos." else "Senha incorreta."
                senha = ""
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Senha necessária") },
        text = {
            Column {
                Text(motivo)
                Spacer(Modifier.padding(6.dp))
                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it; erro = null },
                    label = { Text("Senha da pessoa de confiança") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = erro != null,
                    supportingText = { erro?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { conferir() }, enabled = senha.isNotEmpty() && !conferindo) {
                if (conferindo) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Confirmar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Controla pedidos de senha: chame [ask] com o motivo e a ação a executar. */
class PasswordGate {
    var pendente by mutableStateOf<Pair<String, () -> Unit>?>(null)
    fun ask(motivo: String, acao: () -> Unit) {
        pendente = motivo to acao
    }
}

@Composable
fun rememberPasswordGate(): PasswordGate {
    val gate = remember { PasswordGate() }
    gate.pendente?.let { (motivo, acao) ->
        PasswordDialog(
            motivo = motivo,
            onDismiss = { gate.pendente = null },
            onSuccess = { gate.pendente = null; acao() },
        )
    }
    return gate
}

/** Número que muda toda vez que a tela volta ao primeiro plano (para reler permissões). */
@Composable
fun rememberResumeTick(): Int {
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }
    return tick
}

@Composable
fun SectionTitle(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

/** Cartão de aviso colorido. */
@Composable
fun InfoCard(titulo: String, texto: String, cor: Color, conteudo: @Composable () -> Unit = {}) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (texto.isNotEmpty()) Text(texto, style = MaterialTheme.typography.bodyMedium)
            conteudo()
        }
    }
}

@Composable
fun StatusLine(ok: Boolean, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(if (ok) "✅" else "⚠️")
        Spacer(Modifier.width(8.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}
