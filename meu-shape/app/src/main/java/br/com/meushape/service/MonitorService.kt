package br.com.meushape.service

import android.Manifest
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import br.com.meushape.MainActivity
import br.com.meushape.R
import br.com.meushape.data.PassosDia
import br.com.meushape.data.Repo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Serviço em primeiro plano (notificação discreta) que:
 *  - observa tela ligada/desligada e desbloqueio para detectar o sono;
 *  - conta os passos com o sensor do telefone.
 * O estado fica em SharedPreferences para sobreviver se o sistema reiniciar o serviço.
 */
class MonitorService : Service(), SensorEventListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs by lazy { getSharedPreferences("monitor", MODE_PRIVATE) }
    private var receptorRegistrado = false

    private val receptor = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_USER_PRESENT -> comecouUso()
                Intent.ACTION_SCREEN_OFF -> terminouUso()
                // Tela ligada sem desbloquear (ver a hora, notificação) não conta como uso.
                Intent.ACTION_SCREEN_ON -> if (!estaBloqueado()) comecouUso()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        primeiroPlano()
        registerReceiver(receptor, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        })
        receptorRegistrado = true
        // Se o serviço (re)começou com o telefone em uso, processa a pausa até agora.
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isInteractive && !estaBloqueado()) comecouUso()
        registrarPassos()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        primeiroPlano()
        if (intent?.action == ACAO_PASSOS) registrarPassos()
        return START_STICKY
    }

    override fun onDestroy() {
        if (receptorRegistrado) unregisterReceiver(receptor)
        getSystemService(SensorManager::class.java)?.unregisterListener(this)
        scope.cancel()
        super.onDestroy()
    }

    private fun estaBloqueado() = getSystemService(KeyguardManager::class.java).isKeyguardLocked

    // ---------------- Sono ----------------

    private fun comecouUso() {
        if (prefs.getBoolean("em_uso", false)) return
        val fimUltimo = prefs.getLong("fim_ultimo_uso", 0L)
        val agora = System.currentTimeMillis()
        prefs.edit().putBoolean("em_uso", true).apply()
        if (fimUltimo > 0) {
            scope.launch { Repo.get(this@MonitorService).processarVoltaDeUso(fimUltimo, agora) }
        }
    }

    private fun terminouUso() {
        if (!prefs.getBoolean("em_uso", true)) return
        prefs.edit().putBoolean("em_uso", false).putLong("fim_ultimo_uso", System.currentTimeMillis()).apply()
    }

    // ---------------- Passos ----------------

    private fun registrarPassos() {
        if (Build.VERSION.SDK_INT >= 29 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED
        ) return
        val sm = getSystemService(SensorManager::class.java) ?: return
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        sm.unregisterListener(this)
        // Atualizações em lote a cada ~1 minuto economizam bateria.
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL, 60_000_000)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val valor = event.values[0].toLong()
        val hoje = LocalDate.now().toString()
        val ultimo = prefs.getLong("passos_ultimo_valor", -1L)
        var hojeTotal = if (prefs.getString("passos_dia", "") == hoje) prefs.getInt("passos_hoje", 0) else 0
        // O contador do sensor zera quando o celular reinicia.
        val delta = when {
            ultimo < 0 -> 0L
            valor < ultimo -> valor
            else -> valor - ultimo
        }
        hojeTotal += delta.toInt()
        prefs.edit().putLong("passos_ultimo_valor", valor).putString("passos_dia", hoje)
            .putInt("passos_hoje", hojeTotal).apply()
        scope.launch { Repo.get(this@MonitorService).db.sono().salvarPassos(PassosDia(hoje, hojeTotal)) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ---------------- Notificação fixa ----------------

    private fun primeiroPlano() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL, "Monitor de sono e passos", NotificationManager.IMPORTANCE_MIN).apply {
                setShowBadge(false)
            }
        )
        val abrir = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val n = Notification.Builder(this, CANAL)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle("Meu Shape")
            .setContentText("Acompanhando sono e passos")
            .setOngoing(true)
            .setContentIntent(abrir)
            .build()
        val tipo = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, 77, n, tipo)
    }

    companion object {
        private const val CANAL = "monitor"
        const val ACAO_PASSOS = "passos"

        fun iniciar(ctx: Context, acao: String? = null) {
            try {
                ContextCompat.startForegroundService(ctx, Intent(ctx, MonitorService::class.java).setAction(acao))
            } catch (_: Exception) {
            }
        }

        fun temSensorDePassos(ctx: Context) =
            ctx.getSystemService(SensorManager::class.java)?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
    }
}
