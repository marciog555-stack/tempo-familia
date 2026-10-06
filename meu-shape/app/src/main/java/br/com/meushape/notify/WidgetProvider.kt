package br.com.meushape.notify

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import br.com.meushape.MainActivity
import br.com.meushape.R
import br.com.meushape.data.Repo
import br.com.meushape.data.TipoItem
import br.com.meushape.logic.Horario
import br.com.meushape.logic.SonoCalc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Widget da tela inicial: próxima refeição e horas dormidas nas últimas 24h. */
class WidgetProvider : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, manager: AppWidgetManager, ids: IntArray) = atualizar(ctx)

    companion object {
        fun atualizar(ctx: Context) {
            val app = ctx.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val manager = AppWidgetManager.getInstance(app)
                    val ids = manager.getAppWidgetIds(ComponentName(app, WidgetProvider::class.java))
                    if (ids.isEmpty()) return@launch
                    val repo = Repo.get(app)
                    val agora = LocalDateTime.now()
                    val proximas = Alarmes.proximos(app, agora.minusMinutes(30))
                        .filter { it.item.tipo == TipoItem.REFEICAO }
                    val feitos = repo.db.feitos().listarPeriodo(agora.toLocalDate().minusDays(1).toString(), agora.toLocalDate().plusDays(2).toString())
                    val proxima = proximas.firstOrNull { p -> feitos.none { it.data == p.dia.toString() && it.itemId == p.item.id } }
                    val sono = SonoCalc.ultimas24h(repo.observarSonoConfirmado().first(), agora)

                    val v = RemoteViews(app.packageName, R.layout.widget)
                    v.setTextViewText(
                        R.id.widget_proxima,
                        proxima?.let { "${Horario.texto(it.item.inicioMin)} · ${it.item.titulo}" } ?: "Nenhuma pendente",
                    )
                    v.setTextViewText(R.id.widget_sono, "Sono 24h: %dh %02dmin".format(sono / 60, sono % 60))
                    val abrir = PendingIntent.getActivity(
                        app, 10, Intent(app, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
                    )
                    v.setOnClickPendingIntent(R.id.widget_raiz, abrir)
                    manager.updateAppWidget(ids, v)
                } catch (_: Exception) {
                }
            }
        }
    }
}
