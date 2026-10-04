package br.com.tempofamilia.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import br.com.tempofamilia.MainActivity
import br.com.tempofamilia.R
import br.com.tempofamilia.util.UsageTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Serviço em primeiro plano (com notificação fixa) que verifica a cada 2 segundos
 * qual app está aberto e aplica os limites e horários da família.
 */
class GuardService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        entrarEmPrimeiroPlano()
        scope.launch {
            val power = getSystemService(PowerManager::class.java)
            while (isActive) {
                try {
                    if (power.isInteractive) {
                        UsageTracker.foregroundPackage(this@GuardService)?.let {
                            Enforcer.checkApp(this@GuardService, it)
                        }
                    }
                } catch (_: Exception) {
                }
                delay(2000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        entrarEmPrimeiroPlano()
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun entrarEmPrimeiroPlano() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL, "Proteção ativa", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Mantém o Tempo Família funcionando"
                setShowBadge(false)
            }
        )
        val abrir = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificacao = Notification.Builder(this, CANAL)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle("Tempo Família ativo")
            .setContentText("Protegendo seu tempo com a família")
            .setOngoing(true)
            .setContentIntent(abrir)
            .build()
        val tipo = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, 1, notificacao, tipo)
    }

    companion object {
        private const val CANAL = "protecao"

        fun start(ctx: Context) {
            try {
                ContextCompat.startForegroundService(ctx, Intent(ctx, GuardService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
