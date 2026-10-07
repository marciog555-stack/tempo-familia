package br.com.meushape.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import br.com.meushape.data.ItemPlano
import br.com.meushape.data.Repo
import br.com.meushape.logic.Escala
import br.com.meushape.logic.Horario
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Agenda sempre UM alarme exato: o próximo item da rotina. Quando ele toca, o receptor
 * mostra a notificação e agenda o seguinte. Reagendamos também ao ligar o celular,
 * ao abrir o app e ao editar o cardápio.
 */
object Alarmes {
    const val CANAL_ROTINA = "rotina"
    const val EXTRA_MOMENTO = "momento"

    fun criarCanais(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL_ROTINA, "Refeições e treinos", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Avisos no horário de cada refeição e treino"
            }
        )
    }

    /** Item com o dia lógico ao qual pertence e o momento real em que acontece. */
    data class Agendado(val dia: LocalDate, val item: ItemPlano, val momento: LocalDateTime)

    /** Itens dos próximos dias lógicos, em ordem de horário. */
    suspend fun proximos(ctx: Context, aPartirDe: LocalDateTime, dias: Int = 3): List<Agendado> {
        val repo = Repo.get(ctx)
        val escala = repo.escala()
        val hoje = Escala.diaLogico(aPartirDe)
        val lista = mutableListOf<Agendado>()
        for (i in 0 until dias) {
            val dia = hoje.plusDays(i.toLong())
            repo.planoDoDia(dia, escala).forEach { item ->
                lista += Agendado(dia, item, Horario.momento(dia, item.inicioMin))
            }
        }
        return lista.filter { it.momento.isAfter(aPartirDe) }.sortedBy { it.momento }
    }

    suspend fun agendarProximo(ctx: Context, depoisDe: LocalDateTime = LocalDateTime.now()) {
        val proximo = proximos(ctx, depoisDe).firstOrNull { it.item.notificar } ?: return
        val epoch = proximo.momento.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = Intent(ctx, AlarmeReceiver::class.java).putExtra(EXTRA_MOMENTO, epoch)
        val pi = PendingIntent.getBroadcast(
            ctx, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = ctx.getSystemService(AlarmManager::class.java)
        val podeExato = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (podeExato) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epoch, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epoch, pi)
        }
    }
}
