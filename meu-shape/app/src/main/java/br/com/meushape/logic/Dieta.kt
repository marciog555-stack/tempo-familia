package br.com.meushape.logic

import kotlin.math.roundToInt

data class Macros(val kcal: Int, val proteina: Int, val carbo: Int, val gordura: Int)

/**
 * Sugestão inicial de dieta (estimativa, não substitui nutricionista):
 *  - gasto basal pela fórmula de Mifflin-St Jeor, atividade moderada (x1,45);
 *  - para emagrecer: déficit de 500 kcal (mínimo 1.500 homem / 1.200 mulher);
 *  - proteína 2 g/kg do peso meta, gordura 0,8 g/kg, carboidrato com o restante.
 */
object Dieta {
    fun sugestao(homem: Boolean, idade: Int, alturaCm: Double, pesoKg: Double, metaKg: Double): Macros {
        val basal = 10 * pesoKg + 6.25 * alturaCm - 5 * idade + if (homem) 5 else -161
        val gasto = basal * 1.45
        val minimo = if (homem) 1500.0 else 1200.0
        val kcal = when {
            metaKg < pesoKg -> (gasto - 500).coerceAtLeast(minimo)
            metaKg > pesoKg -> gasto + 300
            else -> gasto
        }
        val proteina = 2.0 * metaKg
        val gordura = 0.8 * metaKg
        val carbo = ((kcal - proteina * 4 - gordura * 9) / 4).coerceAtLeast(50.0)
        val kcalFinal = proteina * 4 + gordura * 9 + carbo * 4
        // Arredonda kcal para múltiplos de 50.
        return Macros(
            kcal = ((kcalFinal / 50).roundToInt() * 50),
            proteina = proteina.roundToInt(),
            carbo = carbo.roundToInt(),
            gordura = gordura.roundToInt(),
        )
    }
}
