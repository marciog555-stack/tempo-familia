package br.com.tempofamilia.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/** Administrador do dispositivo: impede a desinstalação enquanto estiver ativo. */
class AdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "Desativar o administrador permite desinstalar o Tempo Família. " +
            "Faça isso apenas com a pessoa de confiança."
}
