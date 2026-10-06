package br.com.meushape

import br.com.meushape.logic.ProgressoCalc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProgressoTest {
    private fun d(dia: Int, mes: Int = 10) = LocalDate.of(2026, mes, dia)

    @Test
    fun mediasPorSemanaComecamNaSegunda() {
        val m = ProgressoCalc.mediasSemanais(listOf(d(5) to 74.0, d(11) to 73.0, d(12) to 72.0))
        assertEquals(2, m.size)
        assertEquals(d(5), m[0].segunda)
        assertEquals(73.5, m[0].media, 0.001)
        assertEquals(72.0, m[1].media, 0.001)
    }

    @Test
    fun platoQuandoMediaParadaPorDuasSemanas() {
        val parado = listOf(d(5) to 72.0, d(12) to 71.9, d(19) to 72.05)
        assertTrue(ProgressoCalc.plato(ProgressoCalc.mediasSemanais(parado)))
        val caindo = listOf(d(5) to 72.0, d(12) to 71.6, d(19) to 71.2)
        assertFalse(ProgressoCalc.plato(ProgressoCalc.mediasSemanais(caindo)))
        // Semanas com buraco não contam como seguidas.
        val buraco = listOf(d(5) to 72.0, d(12) to 72.0, d(26) to 72.0)
        assertFalse(ProgressoCalc.plato(ProgressoCalc.mediasSemanais(buraco)))
    }

    @Test
    fun ritmoAteAMeta() {
        // 74,2 -> 65 em ~17,6 semanas: ~0,52 kg/semana
        val r = ProgressoCalc.ritmoNecessario(74.2, 65.0, d(6), LocalDate.of(2027, 2, 6))!!
        assertEquals(0.52, r, 0.01)
    }
}
