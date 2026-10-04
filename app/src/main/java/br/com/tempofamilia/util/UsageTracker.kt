package br.com.tempofamilia.util

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId

/**
 * Mede o uso dos apps com o UsageStatsManager, somando os períodos em primeiro plano
 * desde a meia-noite. Como a soma sempre começa à meia-noite, o limite "zera" sozinho.
 */
object UsageTracker {

    fun hasPermission(ctx: Context): Boolean {
        val ops = ctx.getSystemService(AppOpsManager::class.java)
        val modo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        }
        return modo == AppOpsManager.MODE_ALLOWED
    }

    fun startOfToday(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** App que está na frente agora (ou null se a tela está apagada / desconhecido). */
    fun foregroundPackage(ctx: Context): String? {
        val usm = ctx.getSystemService(UsageStatsManager::class.java) ?: return null
        val fim = System.currentTimeMillis()
        val eventos = usm.queryEvents(fim - 3 * 60 * 60 * 1000L, fim) ?: return null
        val e = UsageEvents.Event()
        var atual: String? = null
        while (eventos.hasNextEvent()) {
            eventos.getNextEvent(e)
            when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> atual = e.packageName
                UsageEvents.Event.ACTIVITY_PAUSED -> if (e.packageName == atual) atual = null
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> atual = null
            }
        }
        return atual
    }

    /** Milissegundos de uso hoje, por pacote. */
    fun usageToday(ctx: Context): Map<String, Long> {
        val usm = ctx.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()
        val inicio = startOfToday()
        val fim = System.currentTimeMillis()
        val eventos = usm.queryEvents(inicio, fim) ?: return emptyMap()
        val e = UsageEvents.Event()
        val abertoDesde = HashMap<String, Long>() // pacote -> quando foi para a frente
        val vistos = HashSet<String>()
        val totais = HashMap<String, Long>()
        var naFrente: String? = null

        fun soma(pkg: String, ms: Long) {
            if (ms > 0) totais[pkg] = (totais[pkg] ?: 0L) + ms
        }

        while (eventos.hasNextEvent()) {
            eventos.getNextEvent(e)
            val pkg = e.packageName ?: continue
            when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (pkg !in abertoDesde) abertoDesde[pkg] = e.timeStamp
                    vistos.add(pkg)
                    naFrente = pkg
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val desde = abertoDesde.remove(pkg)
                    if (desde != null) {
                        soma(pkg, e.timeStamp - desde)
                    } else if (pkg !in vistos) {
                        // Já estava aberto à meia-noite.
                        soma(pkg, e.timeStamp - inicio)
                    }
                    vistos.add(pkg)
                    if (pkg == naFrente) naFrente = null
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    abertoDesde.forEach { (p, desde) -> soma(p, e.timeStamp - desde) }
                    abertoDesde.clear()
                    naFrente = null
                }
            }
        }
        // Só o app que está realmente na frente conta o tempo até agora.
        naFrente?.let { p -> abertoDesde[p]?.let { soma(p, fim - it) } }
        return totais
    }

    fun usedTodayMs(ctx: Context, pkg: String): Long = usageToday(ctx)[pkg] ?: 0L
}
