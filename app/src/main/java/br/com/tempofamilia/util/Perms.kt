package br.com.tempofamilia.util

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import br.com.tempofamilia.service.AdminReceiver
import br.com.tempofamilia.service.GuardAccessibilityService

/** Verifica e abre as telas de cada permissão necessária. */
object Perms {

    fun usage(ctx: Context) = UsageTracker.hasPermission(ctx)

    fun overlay(ctx: Context) = Settings.canDrawOverlays(ctx)

    fun accessibility(ctx: Context): Boolean {
        val ativos = Settings.Secure.getString(
            ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val meu = ComponentName(ctx, GuardAccessibilityService::class.java)
        return ativos.split(':').any { ComponentName.unflattenFromString(it) == meu }
    }

    fun adminComponent(ctx: Context) = ComponentName(ctx, AdminReceiver::class.java)

    fun admin(ctx: Context): Boolean =
        ctx.getSystemService(DevicePolicyManager::class.java).isAdminActive(adminComponent(ctx))

    fun battery(ctx: Context): Boolean =
        ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

    fun notifications(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Tudo que é essencial para a proteção funcionar. */
    fun essenciaisOk(ctx: Context) = usage(ctx) && overlay(ctx) && accessibility(ctx)

    // ---- Intents para abrir cada tela ----

    private fun pkgUri(ctx: Context) = Uri.parse("package:${ctx.packageName}")

    fun usageIntent(ctx: Context) = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun overlayIntent(ctx: Context) = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri(ctx))

    fun accessibilityIntent() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun appDetailsIntent(ctx: Context) = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri(ctx))

    fun adminIntent(ctx: Context) = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent(ctx))
        putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "Com o administrador ativo, o Tempo Família não pode ser desinstalado sem antes ser desativado."
        )
    }

    @android.annotation.SuppressLint("BatteryLife")
    fun batteryIntent(ctx: Context) = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkgUri(ctx))

    /** Abre uma tela do sistema; se falhar, abre as Configurações gerais. */
    fun open(ctx: Context, intent: Intent) {
        try {
            ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            try {
                ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
            }
        }
    }
}
