package br.com.tempofamilia.util

import android.os.Build

/** Identifica a marca do celular para mostrar instruções certas (Realme, Samsung, outros). */
object Aparelho {
    private val marca = "${Build.MANUFACTURER} ${Build.BRAND}".lowercase()

    /** Realme, Oppo e OnePlus usam o mesmo sistema (ColorOS / Realme UI). */
    val isRealme = listOf("realme", "oppo", "oneplus").any { marca.contains(it) }
    val isSamsung = marca.contains("samsung")

    /** Escolhe o texto conforme a marca. */
    fun texto(realme: String, samsung: String, outro: String) = when {
        isRealme -> realme
        isSamsung -> samsung
        else -> outro
    }
}
