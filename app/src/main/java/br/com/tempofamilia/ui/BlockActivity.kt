package br.com.tempofamilia.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.tempofamilia.data.Store
import br.com.tempofamilia.service.BlockReason
import br.com.tempofamilia.ui.theme.TempoFamiliaTheme
import br.com.tempofamilia.ui.theme.Verde

/** Tela de bloqueio em tela cheia. O botão Voltar leva para a tela inicial. */
class BlockActivity : ComponentActivity() {

    private var motivo by mutableStateOf(BlockReason.LIMITE)
    private var detalhe by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lerIntent(intent)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = irParaInicio()
        })
        setContent {
            TempoFamiliaTheme {
                BlockScreen(
                    motivo = motivo,
                    detalhe = detalhe,
                    onInicio = { irParaInicio() },
                    onLiberado = {
                        // Libera as telas de configuração por 10 minutos.
                        Store.update { it.copy(unlockedUntil = System.currentTimeMillis() + 10 * 60_000L) }
                        finish()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        lerIntent(intent)
    }

    private fun lerIntent(i: Intent) {
        motivo = runCatching { BlockReason.valueOf(i.getStringExtra(EXTRA_MOTIVO) ?: "") }
            .getOrDefault(BlockReason.LIMITE)
        detalhe = i.getStringExtra(EXTRA_DETALHE) ?: ""
    }

    private fun irParaInicio() {
        try {
            startActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
        finish()
    }

    companion object {
        private const val EXTRA_MOTIVO = "motivo"
        private const val EXTRA_DETALHE = "detalhe"

        fun intent(ctx: Context, motivo: BlockReason, detalhe: String) =
            Intent(ctx, BlockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EXTRA_MOTIVO, motivo.name)
                putExtra(EXTRA_DETALHE, detalhe)
            }
    }
}

@androidx.compose.runtime.Composable
private fun BlockScreen(
    motivo: BlockReason,
    detalhe: String,
    onInicio: () -> Unit,
    onLiberado: () -> Unit,
) {
    var pedirSenha by remember { mutableStateOf(false) }
    val (emoji, titulo, texto) = when (motivo) {
        BlockReason.LIMITE -> Triple(
            "⏰", "Tempo esgotado",
            "Você já usou todo o tempo de hoje no $detalhe. O limite volta a zerar à meia-noite.\n\nQue tal aproveitar esse tempo com seus filhos?"
        )
        BlockReason.HORARIO -> {
            val partes = detalhe.split("|")
            Triple(
                "👨‍👩‍👧‍👦", "Horário da família",
                "Agora é \"${partes.getOrElse(1) { "" }}\". O ${partes.getOrElse(0) { "app" }} fica bloqueado até o fim desse horário.\n\nSolte o celular e aproveite este momento."
            )
        }
        BlockReason.PALAVRA -> Triple(
            "🛡️", "Conteúdo bloqueado",
            "Esta página ou pesquisa foi bloqueada pelo Tempo Família.\n\nLembre-se do porquê você instalou este app: sua família merece o seu melhor."
        )
        BlockReason.CONFIGURACOES -> Triple(
            "🔒", "Configuração protegida",
            "Esta tela pode desativar o Tempo Família, por isso está protegida.\n\nA pessoa de confiança pode liberar o acesso por 10 minutos com a senha."
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Verde)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(emoji, fontSize = 72.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            titulo, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            texto, color = Color.White, style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onInicio,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Verde),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Voltar à tela inicial") }
        if (motivo == BlockReason.CONFIGURACOES) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { pedirSenha = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Liberar com a senha (10 min)", color = Color.White)
            }
        }
    }

    if (pedirSenha) {
        PasswordDialog(
            motivo = "Liberar as configurações protegidas por 10 minutos.",
            onDismiss = { pedirSenha = false },
            onSuccess = { pedirSenha = false; onLiberado() },
        )
    }
}
