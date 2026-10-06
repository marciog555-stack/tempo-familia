package br.com.meushape.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import br.com.meushape.MainActivity
import br.com.meushape.R
import br.com.meushape.data.EstoqueMarmita
import br.com.meushape.data.Marmita

/** Avisos avulsos (estoque de marmitas etc.). */
object Avisos {
    private const val CANAL = "avisos"

    fun estoqueBaixo(ctx: Context, e: EstoqueMarmita) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CANAL, "Avisos", NotificationManager.IMPORTANCE_DEFAULT))
        val nome = if (e.tipo == Marmita.ALMOCO) "almoço" else "janta"
        val abrir = PendingIntent.getActivity(
            ctx, 900, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CANAL)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle("Marmitas de $nome acabando")
            .setContentText("Restam ${e.total} (geladeira ${e.geladeira}, freezer ${e.freezer}). Hora de montar mais!")
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        try { nm.notify(if (e.tipo == Marmita.ALMOCO) 901 else 902, n) } catch (_: SecurityException) {}
    }
}
