package br.com.tempofamilia

import br.com.tempofamilia.data.AppState
import br.com.tempofamilia.util.LiberacaoSemanal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class LiberacaoSemanalTest {
    private fun ms(d: LocalDate, h: Int = 20) = d.atTime(h, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val quarta = LocalDate.of(2026, 10, 7)

    @Test
    fun desligadaNaoLibera() {
        val s = AppState(weeklyReleaseEnabled = false)
        assertFalse(LiberacaoSemanal.disponivel(s, quarta))
        assertFalse(LiberacaoSemanal.emAndamento(LiberacaoSemanal.iniciar(s, ms(quarta)), ms(quarta) + 1000))
    }

    @Test
    fun liberaQuinzeMinutosUmaVezPorSemana() {
        val s = AppState(weeklyReleaseEnabled = true, weeklyReleaseActivatedAt = ms(quarta.minusWeeks(3)))
        assertTrue(LiberacaoSemanal.disponivel(s, quarta))
        val usada = LiberacaoSemanal.iniciar(s, ms(quarta))
        assertTrue(LiberacaoSemanal.emAndamento(usada, ms(quarta) + 14 * 60_000L))
        assertFalse(LiberacaoSemanal.emAndamento(usada, ms(quarta) + 15 * 60_000L))
        // Mesma semana (até domingo): não dá para usar de novo.
        assertFalse(LiberacaoSemanal.disponivel(usada, quarta.plusDays(4)))
        // Segunda seguinte: disponível de novo.
        assertTrue(LiberacaoSemanal.disponivel(usada, quarta.plusDays(5)))
        // Encerrar antes: bloqueio volta na hora.
        assertFalse(LiberacaoSemanal.emAndamento(LiberacaoSemanal.encerrar(usada), ms(quarta) + 60_000L))
    }

    @Test
    fun contaSemanasSemUsar() {
        val ativada = ms(quarta.minusWeeks(3))
        val s = AppState(weeklyReleaseEnabled = true, weeklyReleaseActivatedAt = ativada)
        // Ativada há 3 semanas e nunca usada: 3 semanas completas sem usar.
        assertEquals(3, LiberacaoSemanal.semanasSemUsar(s, quarta))
        // Usada 2 semanas atrás: só a semana passada conta.
        val usada = s.copy(weeklyReleaseUsedWeeks = listOf(LiberacaoSemanal.segunda(quarta.minusWeeks(2)).toString()))
        assertEquals(1, LiberacaoSemanal.semanasSemUsar(usada, quarta))
    }
}

class LiberacaoIntervaloTest {
    private fun ms(d: LocalDate) = d.atTime(20, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun intervaloDeDuasSemanas() {
        val quarta = LocalDate.of(2026, 10, 7)
        val s = AppState(weeklyReleaseEnabled = true, weeklyReleaseIntervalWeeks = 2)
        val usada = LiberacaoSemanal.iniciar(s, ms(quarta))
        assertFalse(LiberacaoSemanal.disponivel(usada, quarta.plusWeeks(1)))  // semana seguinte: não
        assertTrue(LiberacaoSemanal.disponivel(usada, quarta.plusWeeks(2)))   // 2 semanas depois: sim
        assertEquals(LocalDate.of(2026, 10, 19), LiberacaoSemanal.proximaSegunda(usada, quarta))
    }
}
