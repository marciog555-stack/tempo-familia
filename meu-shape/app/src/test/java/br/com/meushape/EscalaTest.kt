package br.com.meushape

import br.com.meushape.logic.AtividadeFolga
import br.com.meushape.logic.Escala
import br.com.meushape.logic.Horario
import br.com.meushape.logic.TipoDia
import br.com.meushape.logic.TrocaDia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class EscalaTest {
    private fun d(dia: Int, mes: Int = 10, ano: Int = 2026) = LocalDate.of(ano, mes, dia)

    @Test
    fun referenciaDoUsuario() {
        val e = Escala()
        assertEquals(TipoDia.FOLGA, e.tipo(d(6)))
        assertEquals(TipoDia.PLANTAO, e.tipo(d(7)))
        assertEquals(TipoDia.FOLGA, e.tipo(d(8)))
        assertEquals(TipoDia.PLANTAO, e.tipo(d(9)))
    }

    @Test
    fun alternaLongeNoFuturoENoPassado() {
        val e = Escala()
        // 06/02/2027 está a 122 dias de 07/10/2026 (par) -> plantão
        assertEquals(TipoDia.PLANTAO, e.tipo(d(6, 2, 2027)))
        assertEquals(TipoDia.FOLGA, e.tipo(d(7, 2, 2027)))
        // passado
        assertEquals(TipoDia.PLANTAO, e.tipo(d(5)))
        assertEquals(TipoDia.FOLGA, e.tipo(d(4)))
        // virada de ano
        assertEquals(TipoDia.FOLGA, e.tipo(d(31, 12, 2026))) // 85 dias (ímpar)
        assertEquals(TipoDia.PLANTAO, e.tipo(d(1, 1, 2027)))
    }

    @Test
    fun treinoECaminhadaAlternamNasFolgas() {
        val e = Escala()
        assertEquals(AtividadeFolga.TREINO, e.atividade(d(6)))
        assertEquals(null, e.atividade(d(7)))
        assertEquals(AtividadeFolga.CAMINHADA, e.atividade(d(8)))
        assertEquals(AtividadeFolga.TREINO, e.atividade(d(10)))
        assertEquals(AtividadeFolga.CAMINHADA, e.atividade(d(4)))
    }

    @Test
    fun trocasManuaisValemPorCima() {
        val trocas = mapOf(
            d(7) to TrocaDia(tipo = TipoDia.FOLGA),          // troca de plantão
            d(9) to TrocaDia(tipo = TipoDia.FERIAS),
            d(10) to TrocaDia(atividade = AtividadeFolga.CAMINHADA),
        )
        val e = Escala(trocas = trocas)
        assertEquals(TipoDia.FOLGA, e.tipo(d(7)))
        assertEquals(TipoDia.FERIAS, e.tipo(d(9)))
        assertTrue(e.tipo(d(9)).usaCardapioFolga)
        assertEquals(TipoDia.PLANTAO, e.tipoAutomatico(d(7)))
        // 06 treino, 07 (virou folga) caminhada, 08 treino
        assertEquals(AtividadeFolga.CAMINHADA, e.atividade(d(7)))
        assertEquals(AtividadeFolga.TREINO, e.atividade(d(8)))
        assertEquals(AtividadeFolga.CAMINHADA, e.atividade(d(10)))
        assertTrue(e.info(d(7)).trocado)
    }

    @Test
    fun periodoDoPlantaoAtravessaAMeiaNoite() {
        val e = Escala()
        val p = e.periodosDePlantao(LocalDateTime.of(2026, 10, 7, 0, 0), LocalDateTime.of(2026, 10, 9, 0, 0))
        assertEquals(listOf(LocalDateTime.of(2026, 10, 7, 17, 40) to LocalDateTime.of(2026, 10, 8, 5, 40)), p)
    }

    @Test
    fun diaLogicoComecaAsSeis() {
        assertEquals(d(7), Escala.diaLogico(LocalDateTime.of(2026, 10, 8, 1, 30)))
        assertEquals(d(8), Escala.diaLogico(LocalDateTime.of(2026, 10, 8, 6, 0)))
        assertEquals(1530, Horario.deTexto(1, 30))
        assertEquals("01:30", Horario.texto(1530))
        assertEquals(LocalDateTime.of(2026, 10, 8, 1, 30), Horario.momento(d(7), 1530))
    }
}

class SemEscalaTest {
    private fun d(dia: Int, mes: Int = 10) = java.time.LocalDate.of(2026, mes, dia)

    @org.junit.Test
    fun semEscalaTodoDiaEhFolgaEAtividadeAlternaTodoDia() {
        val e = Escala(semEscala = true)
        (8..20).forEach { org.junit.Assert.assertEquals(TipoDia.FOLGA, e.tipo(d(it))) }
        org.junit.Assert.assertNotEquals(e.atividade(d(10)), e.atividade(d(11)))
        org.junit.Assert.assertTrue(e.periodosDePlantao(d(8).atStartOfDay(), d(20).atStartOfDay()).isEmpty())
    }

    @org.junit.Test
    fun novaReferenciaDePlantao() {
        val e = Escala(referenciaPlantao = d(20, 11))
        org.junit.Assert.assertEquals(TipoDia.PLANTAO, e.tipo(d(20, 11)))
        org.junit.Assert.assertEquals(TipoDia.FOLGA, e.tipo(d(21, 11)))
        org.junit.Assert.assertEquals(TipoDia.PLANTAO, e.tipo(d(22, 11)))
    }
}
