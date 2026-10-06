package br.com.meushape.data

/** Biblioteca inicial com os exercícios mais comuns de academia. */
object ExerciciosPadrao {
    private val MAPA = linkedMapOf(
        "Peito" to listOf(
            "Supino reto com barra", "Supino reto com halteres", "Supino inclinado com barra",
            "Supino inclinado com halteres", "Supino declinado", "Supino na máquina",
            "Crucifixo com halteres", "Crucifixo inclinado", "Crossover no cabo",
            "Peck deck (voador)", "Flexão de braço", "Pullover",
        ),
        "Costas" to listOf(
            "Puxada frontal (pulldown)", "Puxada com triângulo", "Barra fixa",
            "Remada curvada com barra", "Remada unilateral com halter (serrote)",
            "Remada baixa sentado no cabo", "Remada cavalinho (T-bar)", "Remada na máquina",
            "Pulldown com braços estendidos", "Levantamento terra", "Hiperextensão lombar",
        ),
        "Ombros" to listOf(
            "Desenvolvimento com halteres", "Desenvolvimento com barra", "Desenvolvimento na máquina",
            "Elevação lateral", "Elevação lateral no cabo", "Elevação frontal",
            "Crucifixo invertido", "Face pull", "Remada alta", "Encolhimento com halteres",
        ),
        "Bíceps" to listOf(
            "Rosca direta com barra", "Rosca direta barra W", "Rosca alternada com halteres",
            "Rosca martelo", "Rosca Scott", "Rosca concentrada", "Rosca no cabo",
        ),
        "Tríceps" to listOf(
            "Tríceps pulley com corda", "Tríceps pulley com barra", "Tríceps testa",
            "Tríceps francês", "Tríceps coice", "Mergulho no banco", "Paralelas", "Supino fechado",
        ),
        "Quadríceps" to listOf(
            "Agachamento livre", "Agachamento no Smith", "Agachamento goblet", "Leg press 45°",
            "Hack machine", "Cadeira extensora", "Afundo (passada)", "Agachamento búlgaro",
        ),
        "Posterior" to listOf(
            "Mesa flexora", "Cadeira flexora", "Flexora em pé", "Stiff",
            "Levantamento terra romeno", "Good morning",
        ),
        "Glúteos" to listOf(
            "Elevação pélvica (hip thrust)", "Ponte de glúteo", "Glúteo no cabo (coice)",
            "Glúteo na máquina", "Cadeira abdutora", "Passada lateral com elástico",
        ),
        "Panturrilha" to listOf(
            "Panturrilha em pé", "Panturrilha sentado", "Panturrilha no leg press", "Panturrilha unilateral",
        ),
        "Abdômen" to listOf(
            "Abdominal supra (crunch)", "Abdominal infra (elevação de pernas)", "Abdominal oblíquo",
            "Abdominal remador", "Abdominal na polia", "Prancha", "Prancha lateral",
            "Roda abdominal", "Elevação de pernas na barra",
        ),
        "Cardio" to listOf(
            "Esteira", "Bicicleta ergométrica", "Elíptico (transport)", "Escada", "Remo ergômetro",
            "Pular corda", "HIIT",
        ),
    )

    fun todos(): List<Exercicio> = MAPA.flatMap { (grupo, nomes) -> nomes.map { Exercicio(nome = it, grupo = grupo) } }
}
