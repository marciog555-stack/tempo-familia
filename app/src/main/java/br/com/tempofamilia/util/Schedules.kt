package br.com.tempofamilia.util

import br.com.tempofamilia.data.FamilySchedule
import java.time.LocalDateTime

/** Regras dos horários da família. */
object Schedules {
    val NOMES_DIAS = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
    private val NOMES_DIAS_LONGOS =
        listOf("segunda", "terça", "quarta", "quinta", "sexta", "sábado", "domingo")

    private fun diaAnterior(dia: Int) = if (dia == 1) 7 else dia - 1

    /** A faixa está valendo neste momento? Início igual ao fim = dia inteiro. */
    fun isActive(s: FamilySchedule, agora: LocalDateTime = LocalDateTime.now()): Boolean {
        val dia = agora.dayOfWeek.value
        val min = agora.hour * 60 + agora.minute
        return when {
            s.startMin == s.endMin -> dia in s.days
            s.startMin < s.endMin -> dia in s.days && min >= s.startMin && min < s.endMin
            else -> (dia in s.days && min >= s.startMin) ||
                (diaAnterior(dia) in s.days && min < s.endMin)
        }
    }

    fun activeNow(lista: List<FamilySchedule>): FamilySchedule? = lista.firstOrNull { isActive(it) }

    /** Próximo início de horário nos próximos 7 dias. */
    fun next(lista: List<FamilySchedule>, agora: LocalDateTime = LocalDateTime.now()): Pair<FamilySchedule, LocalDateTime>? {
        var melhor: Pair<FamilySchedule, LocalDateTime>? = null
        for (s in lista) {
            for (d in 0..7) {
                val data = agora.toLocalDate().plusDays(d.toLong())
                if (data.dayOfWeek.value !in s.days) continue
                val inicio = data.atTime(s.startMin / 60, s.startMin % 60)
                if (inicio.isAfter(agora)) {
                    if (melhor == null || inicio.isBefore(melhor.second)) melhor = s to inicio
                    break
                }
            }
        }
        return melhor
    }

    fun hora(min: Int) = "%02d:%02d".format(min / 60, min % 60)

    fun dias(dias: Set<Int>): String = when {
        dias.size == 7 -> "Todos os dias"
        dias == setOf(1, 2, 3, 4, 5) -> "Segunda a sexta"
        dias == setOf(6, 7) -> "Fim de semana"
        else -> dias.sorted().joinToString(", ") { NOMES_DIAS[it - 1] }
    }

    fun quando(data: LocalDateTime, agora: LocalDateTime = LocalDateTime.now()): String {
        val diff = data.toLocalDate().toEpochDay() - agora.toLocalDate().toEpochDay()
        val diaTxt = when (diff) {
            0L -> "hoje"
            1L -> "amanhã"
            else -> NOMES_DIAS_LONGOS[data.dayOfWeek.value - 1]
        }
        return "$diaTxt às ${hora(data.hour * 60 + data.minute)}"
    }
}
