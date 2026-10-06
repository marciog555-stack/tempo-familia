package br.com.meushape

import br.com.meushape.logic.Escala
import br.com.meushape.logic.Periodo
import br.com.meushape.logic.SonoCalc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class SonoTest {
    private fun t(dia: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 10, dia, h, m)

    @Test
    fun pausaCurtaNaoEhSono() {
        assertTrue(SonoCalc.detectar(t(6, 22, 0), t(6, 22, 25), 25, emptyList()).isEmpty())
        assertTrue(SonoCalc.detectar(t(6, 22, 0), t(6, 22, 10), 25, emptyList()).isEmpty())
    }

    @Test
    fun pausaLongaComecaNoUltimoUso() {
        val r = SonoCalc.detectar(t(6, 23, 10), t(7, 6, 30), 25, emptyList())
        assertEquals(listOf(Periodo(t(6, 23, 10), t(7, 6, 30))), r)
        assertEquals(440, r.single().minutos)
    }

    @Test
    fun limiteConfiguravel() {
        assertTrue(SonoCalc.detectar(t(6, 13, 0), t(6, 13, 40), 45, emptyList()).isEmpty())
        assertEquals(1, SonoCalc.detectar(t(6, 13, 0), t(6, 13, 40), 30, emptyList()).size)
    }

    @Test
    fun plantaoNuncaContaComoSono() {
        // Plantão de 07/10: 17:40 até 05:40 de 08/10. Telefone parado das 17:00 às 07:00.
        val escala = Escala()
        val excl = escala.periodosDePlantao(t(7, 17), t(8, 7)).map { Periodo(it.first, it.second) }
        val r = SonoCalc.detectar(t(7, 17, 0), t(8, 7, 0), 25, excl)
        assertEquals(listOf(Periodo(t(7, 17, 0), t(7, 17, 40)), Periodo(t(8, 5, 40), t(8, 7, 0))), r)
    }

    @Test
    fun treinoNuncaContaComoSono() {
        // Treino 07:40–08:40 dentro de uma pausa das 07:00 às 12:00.
        val treino = Periodo(t(7, 7, 40), t(7, 8, 40))
        val r = SonoCalc.detectar(t(7, 7, 0), t(7, 12, 0), 25, listOf(treino))
        assertEquals(listOf(Periodo(t(7, 7, 0), t(7, 7, 40)), Periodo(t(7, 8, 40), t(7, 12, 0))), r)
    }

    @Test
    fun sobraPequenaDepoisDaExclusaoEhDescartada() {
        // Sobram só 10 minutos antes do plantão: não conta.
        val plantao = Periodo(t(7, 17, 40), t(8, 5, 40))
        val r = SonoCalc.detectar(t(7, 17, 30), t(8, 5, 50), 25, listOf(plantao))
        assertTrue(r.isEmpty())
    }

    @Test
    fun totalDasUltimas24hSomaSonoECochiloSemDuplicar() {
        val agora = t(8, 12)
        val registros = listOf(
            Periodo(t(7, 13), t(7, 16)),        // cochilo 3h
            Periodo(t(8, 6), t(8, 10)),         // sono 4h
            Periodo(t(8, 9), t(8, 10, 30)),     // sobreposto: só +30 min
            Periodo(t(7, 8), t(7, 11)),         // fora da janela (que começa 07/10 12:00)
        )
        assertEquals((3 * 60 + 4 * 60 + 30).toLong(), SonoCalc.ultimas24h(registros, agora))
    }

    @Test
    fun registroCortadoPelaJanela() {
        val agora = t(8, 12)
        // Começa antes da janela (07/10 12:00): conta só a parte dentro.
        val r = listOf(Periodo(t(7, 10), t(7, 14)))
        assertEquals(120L, SonoCalc.ultimas24h(r, agora))
    }

    @Test
    fun mediaDaSemana() {
        val agora = t(15, 12)
        val r = (9..15).map { d -> Periodo(t(d, 0), t(d, 7)) } // 7 noites de 7h dentro da janela
        assertEquals(420L, SonoCalc.mediaSemana(r, agora))
        assertTrue(SonoCalc.mediaSemana(r.take(5), agora) < SonoCalc.META_MIN)
    }
}
