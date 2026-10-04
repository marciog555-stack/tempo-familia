package br.com.tempofamilia.service

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import br.com.tempofamilia.data.Store
import br.com.tempofamilia.ui.BlockActivity
import br.com.tempofamilia.util.Schedules
import br.com.tempofamilia.util.UsageTracker

/** Motivos de bloqueio mostrados na tela de bloqueio. */
enum class BlockReason { LIMITE, HORARIO, PALAVRA, CONFIGURACOES }

/** Decide quando bloquear e mostra a tela de bloqueio. */
object Enforcer {
    @Volatile private var ultimoBloqueio = 0L

    /** Se o app deve estar bloqueado agora, devolve (motivo, detalhe). */
    fun motivoParaApp(ctx: Context, pkg: String): Pair<BlockReason, String>? {
        val s = Store.state.value
        if (!s.setupDone || !s.limitsEnabled) return null
        val limite = s.limits.firstOrNull { it.packageName == pkg } ?: return null
        Schedules.activeNow(s.schedules)?.let { horario ->
            return BlockReason.HORARIO to "${limite.label}|${horario.name}"
        }
        val usado = UsageTracker.usedTodayMs(ctx, pkg)
        if (usado >= limite.minutesPerDay * 60_000L) {
            return BlockReason.LIMITE to limite.label
        }
        return null
    }

    /** Confere o app que está na frente e bloqueia se for o caso. */
    fun checkApp(ctx: Context, pkg: String) {
        if (pkg == ctx.packageName) return
        val motivo = motivoParaApp(ctx, pkg) ?: return
        block(ctx, motivo.first, motivo.second)
    }

    /** Manda para a tela inicial e abre a tela de bloqueio por cima. */
    fun block(ctx: Context, motivo: BlockReason, detalhe: String) {
        val agora = SystemClock.elapsedRealtime()
        if (agora - ultimoBloqueio < 1500) return
        ultimoBloqueio = agora
        try {
            ctx.startActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
        try {
            ctx.startActivity(BlockActivity.intent(ctx, motivo, detalhe))
        } catch (_: Exception) {
        }
    }

    /** As telas de configuração estão protegidas neste momento? */
    fun settingsGuardActive(): Boolean {
        val s = Store.state.value
        return s.setupDone && s.settingsGuardEnabled && System.currentTimeMillis() >= s.unlockedUntil
    }
}
