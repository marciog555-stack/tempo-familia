package br.com.tempofamilia.util

import br.com.tempofamilia.data.AppState
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Liberação semanal: uma vez por semana (segunda a domingo), o bloqueio de palavras e sites
 * fica suspenso por 15 minutos. Ativar o recurso exige a senha; desativar é livre.
 * As outras proteções (limites de apps, configurações protegidas) continuam valendo.
 */
object LiberacaoSemanal {
    const val DURACAO_MS = 15 * 60_000L

    fun segunda(d: LocalDate): LocalDate = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private fun data(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

    /** O bloqueio de palavras está suspenso neste momento? */
    fun emAndamento(s: AppState, agora: Long = System.currentTimeMillis()): Boolean =
        s.weeklyReleaseEnabled && s.weeklyReleaseStartedAt > 0 &&
            agora >= s.weeklyReleaseStartedAt && agora < s.weeklyReleaseStartedAt + DURACAO_MS

    fun terminaEm(s: AppState): Long = s.weeklyReleaseStartedAt + DURACAO_MS

    /** Ainda dá para usar a liberação nesta semana (respeitando o intervalo em semanas)? */
    fun disponivel(s: AppState, hoje: LocalDate = LocalDate.now()): Boolean =
        s.weeklyReleaseEnabled && !proximaSegunda(s, hoje).isAfter(segunda(hoje))

    /** Segunda-feira da semana em que a próxima liberação fica disponível. */
    fun proximaSegunda(s: AppState, hoje: LocalDate = LocalDate.now()): LocalDate {
        val ultima = s.weeklyReleaseUsedWeeks.maxOrNull()?.let { LocalDate.parse(it) } ?: return segunda(hoje)
        val intervalo = s.weeklyReleaseIntervalWeeks.coerceAtLeast(1).toLong()
        return ultima.plusWeeks(intervalo)
    }

    /** Começa a liberação agora e marca a semana como usada. */
    fun iniciar(s: AppState, agora: Long = System.currentTimeMillis()): AppState {
        val semana = segunda(data(agora)).toString()
        return s.copy(
            weeklyReleaseStartedAt = agora,
            weeklyReleaseUsedWeeks = (s.weeklyReleaseUsedWeeks + semana).distinct().takeLast(104),
        )
    }

    /** Encerra antes dos 15 minutos (o bloqueio volta na hora). */
    fun encerrar(s: AppState): AppState = s.copy(weeklyReleaseStartedAt = 0L)

    /**
     * Semanas completas seguidas, até a semana passada, em que a liberação NÃO foi usada,
     * contando desde que o recurso foi ativado.
     */
    fun semanasSemUsar(s: AppState, hoje: LocalDate = LocalDate.now()): Int {
        if (s.weeklyReleaseActivatedAt == 0L) return 0
        val inicio = segunda(data(s.weeklyReleaseActivatedAt))
        var semana = segunda(hoje).minusWeeks(1)
        var n = 0
        while (!semana.isBefore(inicio) && semana.toString() !in s.weeklyReleaseUsedWeeks) {
            n++
            semana = semana.minusWeeks(1)
        }
        return n
    }
}
