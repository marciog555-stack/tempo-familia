package br.com.meushape

import br.com.meushape.logic.Dieta
import br.com.meushape.logic.Escala
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DietaTest {
    @Test
    fun sugestaoParaEmagrecerFicaNumaFaixaRazoavel() {
        val m = Dieta.sugestao(homem = true, idade = 28, alturaCm = 168.0, pesoKg = 74.2, metaKg = 65.0)
        assertTrue(m.kcal in 1500..2200)
        assertEquals(130, m.proteina)
        assertEquals(52, m.gordura)
        assertTrue(m.carbo > 50)
    }

    @Test
    fun plantaoDiurnoTerminaNoMesmoDia() {
        val e = Escala(referenciaPlantao = LocalDate.of(2026, 10, 10), inicioPlantao = LocalTime.of(7, 0), fimPlantao = LocalTime.of(19, 0))
        val p = e.periodosDePlantao(LocalDate.of(2026, 10, 10).atStartOfDay(), LocalDate.of(2026, 10, 11).atStartOfDay())
        assertEquals(listOf(LocalDate.of(2026, 10, 10).atTime(7, 0) to LocalDate.of(2026, 10, 10).atTime(19, 0)), p)
        assertEquals("07:00 às 19:00", e.horarioPlantaoTexto)
    }
}
