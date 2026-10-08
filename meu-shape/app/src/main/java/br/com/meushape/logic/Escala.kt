package br.com.meushape.logic

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** Tipo de cada dia da escala. Férias e feriado seguem o cardápio de folga. */
enum class TipoDia(val nome: String) {
    PLANTAO("Plantão"), FOLGA("Folga"), FERIAS("Férias"), FERIADO("Feriado");

    /** Dias que não são plantão usam o cardápio de folga. */
    val usaCardapioFolga get() = this != PLANTAO
}

/** O que fazer de manhã na folga: treinar ou caminhar (alterna a cada folga). */
enum class AtividadeFolga(val nome: String) { TREINO("Treino"), CAMINHADA("Caminhada") }

/** Troca manual de um dia (troca de plantão, férias, feriado) e/ou da atividade da folga. */
data class TrocaDia(val tipo: TipoDia? = null, val atividade: AtividadeFolga? = null)

data class InfoDia(val data: LocalDate, val tipo: TipoDia, val atividade: AtividadeFolga?, val trocado: Boolean)

/**
 * Calcula a escala 12x36: plantão um dia sim, um dia não, a partir de uma data de referência.
 * As trocas manuais valem por cima do cálculo automático.
 */
class Escala(
    val referenciaPlantao: LocalDate = REFERENCIA_PLANTAO,
    private val trocas: Map<LocalDate, TrocaDia> = emptyMap(),
    /** Sem escala (ex.: desempregado, férias longas): todo dia segue a rotina de folga. */
    val semEscala: Boolean = false,
) {
    /** Tipo pelo cálculo automático (sem trocas). */
    fun tipoAutomatico(data: LocalDate): TipoDia {
        if (semEscala) return TipoDia.FOLGA
        val dias = ChronoUnit.DAYS.between(referenciaPlantao, data)
        return if (Math.floorMod(dias, 2L) == 0L) TipoDia.PLANTAO else TipoDia.FOLGA
    }

    fun tipo(data: LocalDate): TipoDia = trocas[data]?.tipo ?: tipoAutomatico(data)

    /**
     * Na folga alterna treino e caminhada. Contamos quantas folgas existem entre a primeira
     * folga de referência (véspera do plantão de referência) e a data: pares treinam, ímpares caminham.
     */
    fun atividade(data: LocalDate): AtividadeFolga? {
        if (!tipo(data).usaCardapioFolga) return null
        trocas[data]?.atividade?.let { return it }
        val inicio = referenciaPlantao.minusDays(1) // 06/10/2026: primeira folga = treino
        var folgas = 0L
        if (!data.isBefore(inicio)) {
            var d = inicio
            while (d.isBefore(data)) {
                if (tipo(d).usaCardapioFolga) folgas++
                d = d.plusDays(1)
            }
        } else {
            var d = data
            while (d.isBefore(inicio)) {
                if (tipo(d).usaCardapioFolga) folgas++
                d = d.plusDays(1)
            }
        }
        return if (folgas % 2 == 0L) AtividadeFolga.TREINO else AtividadeFolga.CAMINHADA
    }

    fun info(data: LocalDate) = InfoDia(data, tipo(data), atividade(data), trocas.containsKey(data))

    /** O plantão de um dia vai das 17:40 desse dia até 05:40 do dia seguinte. */
    fun periodosDePlantao(de: LocalDateTime, ate: LocalDateTime): List<Pair<LocalDateTime, LocalDateTime>> {
        val lista = mutableListOf<Pair<LocalDateTime, LocalDateTime>>()
        var d = de.toLocalDate().minusDays(1)
        while (!d.isAfter(ate.toLocalDate())) {
            if (tipo(d) == TipoDia.PLANTAO) {
                val ini = d.atTime(INICIO_PLANTAO)
                val fim = d.plusDays(1).atTime(FIM_PLANTAO)
                if (fim.isAfter(de) && ini.isBefore(ate)) lista += ini to fim
            }
            d = d.plusDays(1)
        }
        return lista
    }

    companion object {
        val REFERENCIA_PLANTAO: LocalDate = LocalDate.of(2026, 10, 7)
        val INICIO_PLANTAO: LocalTime = LocalTime.of(17, 40)
        val FIM_PLANTAO: LocalTime = LocalTime.of(5, 40)

        /** O "dia" do app começa às 06:00: a janta da empresa à 01:30 ainda é do dia do plantão. */
        const val INICIO_DIA_MIN = 6 * 60

        fun diaLogico(agora: LocalDateTime): LocalDate =
            if (agora.hour * 60 + agora.minute < INICIO_DIA_MIN) agora.toLocalDate().minusDays(1)
            else agora.toLocalDate()
    }
}
