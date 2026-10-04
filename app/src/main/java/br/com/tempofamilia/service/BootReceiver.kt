package br.com.tempofamilia.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Reinicia a proteção quando o celular liga ou quando o app é atualizado. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> GuardService.start(context)
        }
    }
}
