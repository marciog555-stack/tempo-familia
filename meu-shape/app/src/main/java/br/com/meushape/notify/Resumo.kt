package br.com.meushape.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import br.com.meushape.MainActivity
import br.com.meushape.R
import br.com.meushape.data.Repo
import br.com.meushape.data.ms
import br.com.meushape.logic.ProgressoCalc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Resumo semanal enviado aos domingos às 20:00. */
object ResumoSemanal {
    fun agendar(ctx: Context) {
        val agora = LocalDateTime.now()
        var alvo = agora.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(LocalTime.of(20, 0))
        if (!alvo.isAfter(agora)) alvo = alvo.plusWeeks(1)
        val pi = PendingIntent.getBroadcast(
            ctx, 2, Intent(ctx, ResumoReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val am = ctx.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alvo.ms(), pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alvo.ms(), pi)
        }
    }

    fun texto(r: Repo.Resumo): String = buildString {
        appendLine("Peso médio: " + (r.pesoMedio?.let { "%.1f kg".format(Locale("pt", "BR"), it) } ?: "sem registros"))
        appendLine("Treinos feitos: ${r.treinos}")
        appendLine("Refeições cumpridas: ${r.refeicoesFeitas} de ${r.refeicoesPlanejadas}")
        appendLine("Média de sono: %dh %02dmin".format(r.sonoMedioMin / 60, r.sonoMedioMin % 60))
        appendLine("Média de passos: ${r.passosMedios}")
        append("Gasto com compras: " + java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(r.gastoCompras))
    }
}

class ResumoReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val pendente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val r = Repo.get(ctx).resumoSemana(ProgressoCalc.segunda(LocalDate.now()))
                val nm = ctx.getSystemService(NotificationManager::class.java)
                nm.createNotificationChannel(NotificationChannel("resumo", "Resumo semanal", NotificationManager.IMPORTANCE_DEFAULT))
                val abrir = PendingIntent.getActivity(ctx, 3, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
                val texto = ResumoSemanal.texto(r)
                val n = NotificationCompat.Builder(ctx, "resumo")
                    .setSmallIcon(R.drawable.ic_notificacao)
                    .setContentTitle("📊 Resumo da semana")
                    .setContentText(texto.lineSequence().first())
                    .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
                    .setContentIntent(abrir)
                    .setAutoCancel(true)
                    .build()
                try { nm.notify(500, n) } catch (_: SecurityException) {}
                ResumoSemanal.agendar(ctx)
            } finally {
                pendente.finish()
            }
        }
    }
}
