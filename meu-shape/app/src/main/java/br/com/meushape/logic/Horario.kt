package br.com.meushape.logic

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Horários do cardápio em minutos desde a meia-noite do dia lógico.
 * Valores acima de 24h (1440) são da madrugada seguinte: 01:30 do plantão = 1530.
 */
object Horario {
    fun texto(min: Int): String {
        val m = Math.floorMod(min, 1440)
        return "%02d:%02d".format(m / 60, m % 60)
    }

    /** Converte "HH:MM" em minutos; horários antes das 06:00 viram madrugada do dia seguinte. */
    fun deTexto(hora: Int, minuto: Int): Int {
        val m = hora * 60 + minuto
        return if (m < Escala.INICIO_DIA_MIN) m + 1440 else m
    }

    fun momento(dia: LocalDate, min: Int): LocalDateTime = dia.atStartOfDay().plusMinutes(min.toLong())
}
