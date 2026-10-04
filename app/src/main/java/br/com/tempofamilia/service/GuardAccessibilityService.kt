package br.com.tempofamilia.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import br.com.tempofamilia.data.Store
import br.com.tempofamilia.util.KeywordMatcher
import br.com.tempofamilia.util.TextNormalizer
import br.com.tempofamilia.util.Watchlists
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Serviço de acessibilidade. Ele:
 *  1. lê o texto digitado em qualquer app e o conteúdo/URL dos navegadores e buscas,
 *     fechando a página se aparecer um termo proibido;
 *  2. bloqueia, sem a senha, as telas de Configurações que desativariam o app;
 *  3. avisa na hora quando um app limitado é aberto.
 */
class GuardAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var navegadores: Set<String> = Watchlists.BROWSERS
    private val ultimaLeitura = HashMap<String, Long>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        navegadores = Watchlists.BROWSERS + descobrirNavegadores()
        GuardService.start(this)
    }

    /** Qualquer app que abre links https também é tratado como navegador. */
    private fun descobrirNavegadores(): Set<String> = try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://exemplo.com.br"))
        packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName }
            .filter { it != packageName }
            .toSet()
    } catch (_: Exception) {
        emptySet()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        val s = Store.state.value
        if (!s.setupDone) return

        try {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    if (verificarConfiguracoes(pkg, event, forcar = true)) return
                    if (s.limits.any { it.packageName == pkg }) {
                        scope.launch { Enforcer.checkApp(this@GuardAccessibilityService, pkg) }
                    }
                    if (pkg in navegadores) verificarConteudo(pkg, forcar = true)
                }
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    if (verificarConfiguracoes(pkg, event, forcar = false)) return
                    if (pkg in navegadores) verificarConteudo(pkg, forcar = false)
                }
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                    if (!event.isPassword && s.wordBlockEnabled) {
                        val digitado = event.text.joinToString(" ")
                        KeywordMatcher.find(digitado)?.let { bloquearPalavra(it) }
                    }
                }
            }
        } catch (_: Exception) {
            // Nunca deixar o serviço cair por causa de uma tela estranha.
        }
    }

    /** Limita leituras pesadas da tela a uma a cada 400 ms por app. */
    private fun podeLer(chave: String, forcar: Boolean): Boolean {
        val agora = SystemClock.elapsedRealtime()
        val ultima = ultimaLeitura[chave] ?: 0L
        if (!forcar && agora - ultima < 400) return false
        ultimaLeitura[chave] = agora
        return true
    }

    /** Devolve true se bloqueou. */
    private fun verificarConfiguracoes(pkg: String, event: AccessibilityEvent, forcar: Boolean): Boolean {
        if (pkg !in Watchlists.SETTINGS_PACKAGES) return false
        if (!Enforcer.settingsGuardActive()) return false

        if (pkg in Watchlists.ACCESSIBILITY_PACKAGES) {
            bloquearConfiguracoes()
            return true
        }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            Watchlists.isProtectedSettingsClass(event.className?.toString() ?: "")
        ) {
            bloquearConfiguracoes()
            return true
        }
        if (!podeLer("cfg:$pkg", forcar)) return false
        val raiz = rootInActiveWindow ?: return false
        if (raiz.packageName?.toString() != pkg) return false
        val texto = TextNormalizer.normalize(lerTexto(raiz))
        if (Watchlists.isProtectedSettingsScreen(texto)) {
            bloquearConfiguracoes()
            return true
        }
        return false
    }

    private fun verificarConteudo(pkg: String, forcar: Boolean) {
        if (!Store.state.value.wordBlockEnabled) return
        if (!podeLer("nav:$pkg", forcar)) return
        val raiz = rootInActiveWindow ?: return
        if (raiz.packageName?.toString() != pkg) return
        KeywordMatcher.find(lerTexto(raiz))?.let { bloquearPalavra(it) }
    }

    /** Junta todo o texto visível da janela (com limites para não pesar). */
    private fun lerTexto(raiz: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        val pilha = ArrayDeque<AccessibilityNodeInfo>()
        pilha.addLast(raiz)
        var contagem = 0
        while (pilha.isNotEmpty() && contagem < 2500 && sb.length < 40_000) {
            val no = pilha.removeLast()
            contagem++
            no.text?.let { sb.append(it).append(' ') }
            no.contentDescription?.let { sb.append(it).append(' ') }
            for (i in 0 until no.childCount) {
                no.getChild(i)?.let { pilha.addLast(it) }
            }
        }
        return sb.toString()
    }

    private fun bloquearPalavra(termo: String) {
        performGlobalAction(GLOBAL_ACTION_BACK)
        performGlobalAction(GLOBAL_ACTION_HOME)
        Enforcer.block(this, BlockReason.PALAVRA, termo)
    }

    private fun bloquearConfiguracoes() {
        performGlobalAction(GLOBAL_ACTION_BACK)
        performGlobalAction(GLOBAL_ACTION_HOME)
        Enforcer.block(this, BlockReason.CONFIGURACOES, "")
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
