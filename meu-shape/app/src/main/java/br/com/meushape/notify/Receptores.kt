package br.com.meushape.notify

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import br.com.meushape.MainActivity
import br.com.meushape.R
import br.com.meushape.data.Repo
import br.com.meushape.data.TipoItem
import br.com.meushape.logic.Horario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Roda um trabalho curto em segundo plano dentro de um BroadcastReceiver. */
private fun BroadcastReceiver.emSegundoPlano(bloco: suspend () -> Unit) {
    val pendente = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try { bloco() } finally { pendente.finish() }
    }
}

/** Toca no horário do item: mostra a notificação e agenda o próximo. */
class AlarmeReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = emSegundoPlano {
        val epoch = intent.getLongExtra(Alarmes.EXTRA_MOMENTO, 0L)
        val momento = LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault())
        // Todos os itens marcados para este mesmo minuto.
        val agora = Alarmes.proximos(ctx, momento.minusSeconds(1))
            .filter { it.momento == momento && it.item.notificar }
        val feitos = Repo.get(ctx).db.feitos()
        agora.forEach { a ->
            val jaFeito = feitos.listarPeriodo(a.dia.toString(), a.dia.toString()).any { it.itemId == a.item.id }
            if (!jaFeito) mostrar(ctx, a)
        }
        Alarmes.agendarProximo(ctx, momento)
    }

    private fun mostrar(ctx: Context, a: Alarmes.Agendado) {
        Alarmes.criarCanais(ctx)
        val id = (a.dia.toEpochDay() * 1000 + a.item.id).toInt()
        val abrir = PendingIntent.getActivity(
            ctx, id, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val titulo = "${Horario.texto(a.item.inicioMin)} · ${a.item.titulo}"
        val n = NotificationCompat.Builder(ctx, Alarmes.CANAL_ROTINA)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle(titulo)
            .setContentText(a.item.descricao)
            .setStyle(NotificationCompat.BigTextStyle().bigText(a.item.descricao))
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        // Botão "Feito" direto na notificação (itens com opções abrem o app para escolher).
        if (a.item.opcoes.isEmpty() && a.item.tipo != TipoItem.TREINO_FOLGA) {
            val feito = PendingIntent.getBroadcast(
                ctx, id,
                Intent(ctx, FeitoReceiver::class.java)
                    .putExtra("data", a.dia.toString())
                    .putExtra("itemId", a.item.id)
                    .putExtra("notifId", id),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            n.addAction(0, "Feito ✓", feito)
        }
        try {
            ctx.getSystemService(NotificationManager::class.java).notify(id, n.build())
        } catch (_: SecurityException) {
        }
    }
}

/** Botão "Feito" da notificação. */
class FeitoReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = emSegundoPlano {
        val data = intent.getStringExtra("data") ?: return@emSegundoPlano
        val itemId = intent.getLongExtra("itemId", 0)
        Repo.get(ctx).marcarFeito(LocalDate.parse(data), itemId, "")
        ctx.getSystemService(NotificationManager::class.java).cancel(intent.getIntExtra("notifId", 0))
    }
}

/** Reagenda os alarmes quando o celular liga, o app é atualizado ou a hora muda. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = emSegundoPlano {
        Repo.get(ctx).prepararPrimeiraVez()
        Alarmes.agendarProximo(ctx)
    }
}
