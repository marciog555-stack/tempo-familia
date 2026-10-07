package br.com.meushape.data

/**
 * Treino de adaptação pronto, para quem está começando (ou voltando) e ainda não tem
 * um treino passado pelo instrutor. Corpo inteiro, em duas partes (A e B) para alternar.
 * Cargas começam vazias: use um peso leve, que dê para fazer todas as repetições com folga.
 */
object TreinoAdaptacao {
    data class Linha(val exercicio: String, val grupo: String, val series: Int, val reps: String, val descanso: Int = 60)

    val A = listOf(
        Linha("Esteira", "Cardio", 1, "10 min (aquecimento)", 30),
        Linha("Leg press 45°", "Quadríceps", 3, "15"),
        Linha("Cadeira extensora", "Quadríceps", 3, "12-15"),
        Linha("Mesa flexora", "Posterior", 3, "12-15"),
        Linha("Supino na máquina", "Peito", 3, "12-15"),
        Linha("Puxada frontal (pulldown)", "Costas", 3, "12-15"),
        Linha("Desenvolvimento na máquina", "Ombros", 3, "12-15"),
        Linha("Prancha", "Abdômen", 3, "30 s", 45),
    )

    val B = listOf(
        Linha("Bicicleta ergométrica", "Cardio", 1, "10 min (aquecimento)", 30),
        Linha("Agachamento goblet", "Quadríceps", 3, "12-15"),
        Linha("Cadeira flexora", "Posterior", 3, "12-15"),
        Linha("Panturrilha em pé", "Panturrilha", 3, "15", 45),
        Linha("Peck deck (voador)", "Peito", 3, "12-15"),
        Linha("Remada baixa sentado no cabo", "Costas", 3, "12-15"),
        Linha("Elevação lateral", "Ombros", 3, "12-15"),
        Linha("Rosca alternada com halteres", "Bíceps", 2, "12-15"),
        Linha("Tríceps pulley com barra", "Tríceps", 2, "12-15"),
        Linha("Abdominal supra (crunch)", "Abdômen", 3, "15", 45),
    )

    const val EXPLICACAO =
        "A fase de adaptação dura cerca de 4 semanas. Ela prepara músculos, tendões e articulações " +
            "e ensina os movimentos antes de treinos mais pesados.\n\n" +
            "• Alterne: um dia Adaptação A, no outro Adaptação B.\n" +
            "• Use carga leve: você deve terminar as repetições com folga, sem chegar à falha.\n" +
            "• Foque na execução correta e controlada.\n" +
            "• Peça ao instrutor da academia para conferir os exercícios e ajustar os aparelhos.\n\n" +
            "Depois da adaptação, monte o treino que o instrutor passar (A, B, C…)."
}
