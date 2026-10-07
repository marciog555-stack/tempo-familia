package br.com.meushape

import br.com.meushape.data.HorarioDia
import br.com.meushape.data.ItemPlano
import br.com.meushape.data.Repo
import br.com.meushape.data.TipoItem
import org.junit.Assert.assertEquals
import org.junit.Test

class HorarioDiaTest {
    @Test
    fun horarioSoDoDiaSubstituiEReordena() {
        val cafe = ItemPlano(id = 1, tipoDia = "PLANTAO", inicioMin = 7 * 60, tipo = TipoItem.REFEICAO, titulo = "Café")
        val treino = ItemPlano(id = 2, tipoDia = "PLANTAO", inicioMin = 7 * 60 + 40, fimMin = 8 * 60 + 40, tipo = TipoItem.TREINO, titulo = "Treino")
        val almoco = ItemPlano(id = 3, tipoDia = "PLANTAO", inicioMin = 12 * 60, tipo = TipoItem.REFEICAO, titulo = "Almoço")
        // Hoje o treino vai ser às 14:00–15:00.
        val r = Repo.aplicarHorarios(listOf(cafe, treino, almoco), listOf(HorarioDia("2026-10-07", 2, 14 * 60, 15 * 60)))
        assertEquals(listOf(1L, 3L, 2L), r.map { it.id })
        assertEquals(14 * 60, r.last().inicioMin)
        assertEquals(15 * 60, r.last().fimMin)
        // Sem ajustes, nada muda.
        assertEquals(listOf(cafe, treino, almoco), Repo.aplicarHorarios(listOf(cafe, treino, almoco), emptyList()))
    }
}
