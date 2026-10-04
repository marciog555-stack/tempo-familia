package br.com.tempofamilia.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import br.com.tempofamilia.data.Password
import br.com.tempofamilia.data.Store
import br.com.tempofamilia.service.GuardService
import br.com.tempofamilia.ui.InfoCard
import br.com.tempofamilia.ui.StatusLine
import br.com.tempofamilia.ui.rememberResumeTick
import br.com.tempofamilia.util.Perms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Um passo de permissão da configuração inicial. */
private data class Passo(
    val titulo: String,
    val explicacao: String,
    val botao: String,
    val obrigatorio: Boolean,
    val concedido: (Context) -> Boolean,
    val abrir: (Context) -> Intent?,
)

private fun passosDePermissao(): List<Passo> = listOf(
    Passo(
        titulo = "Acesso ao uso",
        explicacao = "Permite medir quanto tempo você passa em cada app.\n\n" +
            "Na lista que vai abrir, toque em \"Tempo Família\" e ative \"Permitir acesso ao uso\".",
        botao = "Abrir Acesso ao uso",
        obrigatorio = true,
        concedido = Perms::usage,
        abrir = Perms::usageIntent,
    ),
    Passo(
        titulo = "Sobreposição a outros apps",
        explicacao = "Permite mostrar a tela de bloqueio por cima dos outros apps.\n\n" +
            "Ative \"Permitir sobreposição\" (na Samsung: \"Aparecer por cima\").",
        botao = "Abrir Sobreposição",
        obrigatorio = true,
        concedido = Perms::overlay,
        abrir = Perms::overlayIntent,
    ),
    Passo(
        titulo = "Permitir configurações restritas",
        explicacao = "No Android 13 ou mais novo, apps instalados por APK não podem ativar a " +
            "Acessibilidade até você liberar as \"configurações restritas\".\n\n" +
            "1. Primeiro tente ativar a Acessibilidade no próximo passo. Se aparecer o aviso " +
            "\"Configuração restrita\", toque em OK.\n" +
            "2. Volte aqui e toque em \"Abrir página do app\".\n" +
            "3. No Samsung, toque nos três pontinhos (⋮) no canto superior direito e escolha " +
            "\"Permitir configurações restritas\". Confirme com sua digital ou PIN.\n" +
            "4. Volte e ative a Acessibilidade.\n\n" +
            "Se o seu Android for mais antigo ou a opção não aparecer, pode seguir em frente.",
        botao = "Abrir página do app",
        obrigatorio = false,
        concedido = { Build.VERSION.SDK_INT < 33 || Perms.accessibility(it) },
        abrir = Perms::appDetailsIntent,
    ),
    Passo(
        titulo = "Acessibilidade",
        explicacao = "É o coração da proteção: bloqueia pesquisas e sites pornográficos e protege " +
            "as configurações contra desativação.\n\n" +
            "No Samsung: Acessibilidade → Apps instalados (ou \"Serviços instalados\") → " +
            "Tempo Família → ative a chave e toque em \"Permitir\".\n\n" +
            "Se a chave estiver cinza ou aparecer \"Configuração restrita\", volte ao passo anterior.",
        botao = "Abrir Acessibilidade",
        obrigatorio = true,
        concedido = Perms::accessibility,
        abrir = { Perms.accessibilityIntent() },
    ),
    Passo(
        titulo = "Administrador do dispositivo",
        explicacao = "Dificulta a desinstalação: o app só pode ser removido depois de desativar o " +
            "administrador, e essa tela fica protegida pela senha.\n\nToque em \"Ativar\".",
        botao = "Ativar administrador",
        obrigatorio = false,
        concedido = Perms::admin,
        abrir = Perms::adminIntent,
    ),
    Passo(
        titulo = "Ignorar otimização de bateria",
        explicacao = "Evita que o sistema feche o Tempo Família para economizar bateria. Toque em \"Permitir\".\n\n" +
            "Dica Samsung: em Configurações → Bateria → Limites de uso em segundo plano → " +
            "\"Apps que nunca são suspensos\", adicione o Tempo Família.",
        botao = "Permitir em segundo plano",
        obrigatorio = false,
        concedido = Perms::battery,
        abrir = Perms::batteryIntent,
    ),
)

@Composable
fun SetupScreen() {
    val ctx = LocalContext.current
    val estado by Store.state.collectAsState()
    var etapa by rememberSaveable { mutableIntStateOf(0) }
    val tick = rememberResumeTick()
    val passos = remember { passosDePermissao() }
    // Etapas: 0 boas-vindas, 1 senha, 2..(2+n-1) permissões, depois notificações, depois fim
    val etapaNotif = 2 + passos.size
    val etapaFim = etapaNotif + 1
    val total = etapaFim + 1

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Configuração inicial", style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(progress = { (etapa + 1f) / total }, modifier = Modifier.fillMaxWidth())

        when {
            etapa == 0 -> BoasVindas { etapa = 1 }
            etapa == 1 -> CriarSenha(jaTemSenha = estado.hasPassword) { etapa = 2 }
            etapa < etapaNotif -> {
                val p = passos[etapa - 2]
                // Relê o estado da permissão sempre que o usuário volta ao app.
                val ok = remember(tick, etapa) { p.concedido(ctx) }
                PassoPermissao(
                    passo = p,
                    concedido = ok,
                    onAbrir = { p.abrir(ctx)?.let { Perms.open(ctx, it) } },
                    onVoltar = { etapa-- },
                    onProximo = { etapa++ },
                )
            }
            etapa == etapaNotif -> Notificacoes(onVoltar = { etapa-- }, onProximo = { etapa++ })
            else -> Concluir(ctx, tick, onVoltar = { etapa-- })
        }
    }
}

@Composable
private fun BoasVindas(onProximo: () -> Unit) {
    Text("Bem-vindo ao Tempo Família", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text(
        "Este app vai ajudar você a:\n\n" +
            "• limitar o tempo nas redes sociais;\n" +
            "• bloquear pesquisas e sites pornográficos;\n" +
            "• reservar horários só para a família.\n\n" +
            "Tudo fica guardado apenas neste celular. O app não usa internet, não tem anúncios " +
            "e não envia nenhum dado.\n\n" +
            "Para funcionar de verdade, peça a uma pessoa de confiança (cônjuge, amigo, pastor, " +
            "mentor) para criar a senha no próximo passo. Só ela poderá afrouxar as regras.",
        style = MaterialTheme.typography.bodyLarge,
    )
    Button(onClick = onProximo, modifier = Modifier.fillMaxWidth()) { Text("Começar") }
}

@Composable
private fun CriarSenha(jaTemSenha: Boolean, onProximo: () -> Unit) {
    var senha by remember { mutableStateOf("") }
    var repetir by remember { mutableStateOf("") }
    var salvando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val erro = when {
        senha.isNotEmpty() && senha.length < 6 -> "Use pelo menos 6 caracteres."
        repetir.isNotEmpty() && repetir != senha -> "As senhas não são iguais."
        else -> null
    }

    Text("Senha da pessoa de confiança", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(
        "Entregue o celular para a pessoa de confiança. Ela deve criar uma senha que você NÃO saiba.\n\n" +
            "A senha será pedida para aumentar limites, remover apps ou termos, alterar horários e " +
            "desativar qualquer proteção. Deixar as regras mais rígidas nunca pede senha.",
        style = MaterialTheme.typography.bodyLarge,
    )
    if (jaTemSenha) {
        InfoCard("Senha já criada", "Você pode seguir em frente ou criar uma nova senha abaixo.", MaterialTheme.colorScheme.primaryContainer)
    }
    OutlinedTextField(
        value = senha, onValueChange = { senha = it },
        label = { Text("Nova senha") }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = repetir, onValueChange = { repetir = it },
        label = { Text("Repita a senha") }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        isError = erro != null,
        supportingText = { erro?.let { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = {
            salvando = true
            scope.launch {
                withContext(Dispatchers.Default) { Password.setNew(senha) }
                salvando = false
                onProximo()
            }
        },
        enabled = senha.length >= 6 && senha == repetir && !salvando,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(if (salvando) "Salvando..." else "Salvar senha") }
    if (jaTemSenha) {
        TextButton(onClick = onProximo, modifier = Modifier.fillMaxWidth()) { Text("Manter a senha atual") }
    }
}

@Composable
private fun PassoPermissao(
    passo: Passo,
    concedido: Boolean,
    onAbrir: () -> Unit,
    onVoltar: () -> Unit,
    onProximo: () -> Unit,
) {
    Text(passo.titulo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(passo.explicacao, style = MaterialTheme.typography.bodyLarge)
    StatusLine(concedido, if (concedido) "Concedido" else "Ainda não concedido")
    Button(onClick = onAbrir, modifier = Modifier.fillMaxWidth()) { Text(passo.botao) }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onVoltar, modifier = Modifier.weight(1f)) { Text("Voltar") }
        Button(
            onClick = onProximo,
            enabled = concedido || !passo.obrigatorio,
            modifier = Modifier.weight(1f),
        ) { Text(if (concedido || passo.obrigatorio) "Próximo" else "Pular") }
    }
    if (passo.obrigatorio && !concedido) {
        Text(
            "Este passo é obrigatório para a proteção funcionar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun Notificacoes(onVoltar: () -> Unit, onProximo: () -> Unit) {
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onProximo() }
    Text("Notificações", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(
        "O Tempo Família mostra uma notificação discreta e fixa enquanto protege o celular. " +
            "Isso ajuda o sistema a não fechar o app.",
        style = MaterialTheme.typography.bodyLarge,
    )
    Button(
        onClick = {
            if (Build.VERSION.SDK_INT >= 33) pedir.launch(Manifest.permission.POST_NOTIFICATIONS) else onProximo()
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Permitir notificações") }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onVoltar, modifier = Modifier.weight(1f)) { Text("Voltar") }
        OutlinedButton(onClick = onProximo, modifier = Modifier.weight(1f)) { Text("Pular") }
    }
}

@Composable
private fun Concluir(ctx: Context, tick: Int, onVoltar: () -> Unit) {
    val itens = remember(tick) {
        listOf(
            Perms.usage(ctx) to "Acesso ao uso",
            Perms.overlay(ctx) to "Sobreposição a outros apps",
            Perms.accessibility(ctx) to "Acessibilidade",
            Perms.admin(ctx) to "Administrador do dispositivo",
            Perms.battery(ctx) to "Sem otimização de bateria",
            Perms.notifications(ctx) to "Notificações",
        )
    }
    Text("Tudo pronto!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    itens.forEach { (ok, nome) -> StatusLine(ok, nome) }
    Text(
        "Ao concluir, a proteção começa a valer: as telas de Configurações que desativam o app " +
            "ficarão bloqueadas e só a senha libera.\n\n" +
            "Depois, adicione os apps que quer limitar na aba \"Apps\" e os horários da família na aba \"Horários\".",
        style = MaterialTheme.typography.bodyLarge,
    )
    Button(
        onClick = {
            Store.update { it.copy(setupDone = true) }
            GuardService.start(ctx)
        },
        enabled = Perms.essenciaisOk(ctx) && Store.state.value.hasPassword,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Concluir e ativar a proteção") }
    OutlinedButton(onClick = onVoltar, modifier = Modifier.fillMaxWidth()) { Text("Voltar") }
}
