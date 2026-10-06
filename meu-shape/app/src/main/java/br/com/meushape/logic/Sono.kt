package br.com.meushape.logic

import java.time.Duration
import java.time.LocalDateTime

/** Intervalo de tempo [ini, fim). */
data class Periodo(val ini: LocalDateTime, val fim: LocalDateTime) {
    val minutos: Long get() = Duration.between(ini, fim).toMinutes()
}

/**
 * Regras do sono:
 *  - "Uso" do telefone = do desbloqueio até a tela apagar.
 *  - Se o telefone ficou mais que [limiteMin] sem uso, conta como sono a partir do último uso.
 *  - Períodos de plantão e de treino (e registros já existentes) nunca contam como sono.
 *  - Só registros confirmados entram nos totais.
 */
object SonoCalc {

    /** Tira de [p] todos os trechos que caem dentro de [exclusoes]. */
    fun subtrair(p: Periodo, exclusoes: List<Periodo>): List<Periodo> {
        var partes = listOf(p)
        for (e in exclusoes) {
            partes = partes.flatMap { parte ->
                if (!e.fim.isAfter(parte.ini) || !e.ini.isBefore(parte.fim)) listOf(parte)
                else buildList {
                    if (e.ini.isAfter(parte.ini)) add(Periodo(parte.ini, e.ini))
                    if (e.fim.isBefore(parte.fim)) add(Periodo(e.fim, parte.fim))
                }
            }
        }
        return partes
    }

    /**
     * Sono detectado entre o fim do último uso e o início do uso atual.
     * Devolve vazio se a pausa foi curta. Trechos que sobram após as exclusões
     * só contam se também forem maiores que o limite.
     */
    fun detectar(
        fimUltimoUso: LocalDateTime,
        voltouAUsar: LocalDateTime,
        limiteMin: Int,
        exclusoes: List<Periodo>,
    ): List<Periodo> {
        val pausa = Periodo(fimUltimoUso, voltouAUsar)
        if (pausa.minutos <= limiteMin) return emptyList()
        return subtrair(pausa, exclusoes).filter { it.minutos > limiteMin }
    }

    /** Junta períodos que se sobrepõem. */
    fun unir(lista: List<Periodo>): List<Periodo> {
        val ordenada = lista.filter { it.fim.isAfter(it.ini) }.sortedBy { it.ini }
        val saida = mutableListOf<Periodo>()
        for (p in ordenada) {
            val ultimo = saida.lastOrNull()
            if (ultimo != null && !p.ini.isAfter(ultimo.fim)) {
                if (p.fim.isAfter(ultimo.fim)) saida[saida.lastIndex] = Periodo(ultimo.ini, p.fim)
            } else saida += p
        }
        return saida
    }

    /** Minutos dormidos dentro da janela [de, ate) (sono + cochilos, sem contar duas vezes). */
    fun minutosNaJanela(registros: List<Periodo>, de: LocalDateTime, ate: LocalDateTime): Long =
        unir(registros).sumOf { p ->
            val ini = if (p.ini.isBefore(de)) de else p.ini
            val fim = if (p.fim.isAfter(ate)) ate else p.fim
            if (fim.isAfter(ini)) Duration.between(ini, fim).toMinutes() else 0L
        }

    fun ultimas24h(registros: List<Periodo>, agora: LocalDateTime) =
        minutosNaJanela(registros, agora.minusHours(24), agora)

    /** Média diária dos últimos 7 dias (em minutos). */
    fun mediaSemana(registros: List<Periodo>, agora: LocalDateTime) =
        minutosNaJanela(registros, agora.minusDays(7), agora) / 7

    const val META_MIN = 7 * 60L
}
