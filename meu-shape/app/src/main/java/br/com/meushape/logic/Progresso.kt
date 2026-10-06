package br.com.meushape.logic

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Média do peso de uma semana (segunda a domingo). */
data class MediaSemanal(val segunda: LocalDate, val media: Double, val registros: Int)

object ProgressoCalc {

    fun segunda(d: LocalDate): LocalDate = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun mediasSemanais(pesos: List<Pair<LocalDate, Double>>): List<MediaSemanal> =
        pesos.groupBy { segunda(it.first) }
            .map { (seg, l) -> MediaSemanal(seg, l.map { it.second }.average(), l.size) }
            .sortedBy { it.segunda }

    /**
     * Platô: as médias das 3 últimas semanas seguidas (2 semanas de intervalo)
     * variam menos que [tolerancia] kg.
     */
    fun plato(medias: List<MediaSemanal>, tolerancia: Double = 0.2): Boolean {
        if (medias.size < 3) return false
        val ult = medias.takeLast(3)
        val seguidas = ChronoUnit.WEEKS.between(ult[0].segunda, ult[2].segunda) == 2L
        if (!seguidas) return false
        val valores = ult.map { it.media }
        return valores.max() - valores.min() < tolerancia
    }

    /** Quanto precisa perder por semana para chegar à meta na data. */
    fun ritmoNecessario(pesoAtual: Double, meta: Double, hoje: LocalDate, dataMeta: LocalDate): Double? {
        val semanas = ChronoUnit.DAYS.between(hoje, dataMeta) / 7.0
        if (semanas <= 0) return null
        return (pesoAtual - meta) / semanas
    }
}
