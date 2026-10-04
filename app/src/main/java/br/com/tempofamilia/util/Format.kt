package br.com.tempofamilia.util

/** Formata minutos como "1h 05min" ou "25min". */
fun formatMinutos(min: Long): String {
    val m = min.coerceAtLeast(0)
    return if (m >= 60) "%dh %02dmin".format(m / 60, m % 60) else "${m}min"
}
