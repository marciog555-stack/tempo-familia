package br.com.meushape.data

/** Cardápio e rotina iniciais. Tudo pode ser editado depois no app. */
object PlanoPadrao {
    private fun h(hora: Int, min: Int = 0) = hora * 60 + min

    val REGRA_DO_PRATO =
        "Regra do prato:\n" +
            "• Metade do prato: salada e legumes\n" +
            "• Proteína do tamanho da palma da mão (assada, grelhada ou cozida)\n" +
            "• Arroz e feijão juntos do tamanho de um punho\n" +
            "• Sem fritura, sobremesa, suco ou refrigerante"

    private const val ALMOCO =
        "150 g de frango ou carne magra, 100 g de arroz, 1 concha de feijão, salada"
    private const val CAFE = "3 ovos, 2 fatias de pão integral, 1 fruta"
    private const val LANCHE_OPCOES = "Iogurte natural com whey|3 ovos cozidos|1 lata de atum"

    fun plantao() = listOf(
        ItemPlano(tipoDia = "PLANTAO", inicioMin = h(7), tipo = TipoItem.REFEICAO,
            titulo = "Pré-treino", descricao = "Banana e café sem açúcar"),
        ItemPlano(tipoDia = "PLANTAO", inicioMin = h(7, 40), fimMin = h(8, 40), tipo = TipoItem.TREINO,
            titulo = "Treino", descricao = "Academia"),
        ItemPlano(tipoDia = "PLANTAO", inicioMin = h(9), tipo = TipoItem.REFEICAO,
            titulo = "Café da manhã", descricao = CAFE),
        ItemPlano(tipoDia = "PLANTAO", inicioMin = h(12), tipo = TipoItem.REFEICAO,
            titulo = "Almoço", descricao = ALMOCO, marmita = Marmita.ALMOCO),
        ItemPlano(tipoDia = "PLANTAO", inicioMin = h(13), fimMin = h(16), tipo = TipoItem.DESCANSO,
            titulo = "Cochilo", descricao = "Descansar antes do plantão"),
        ItemPlano(tipoDia = "PLANTAO", inicioMin = h(16, 30), tipo = TipoItem.REFEICAO,
            titulo = "Lanche", descricao = "Escolha uma opção", opcoes = LANCHE_OPCOES),
        ItemPlano(tipoDia = "PLANTAO", inicioMin = h(21), tipo = TipoItem.REFEICAO,
            titulo = "Fruta", descricao = "1 fruta"),
        ItemPlano(tipoDia = "PLANTAO", inicioMin = h(25, 30), tipo = TipoItem.REFEICAO,
            titulo = "Janta da empresa", descricao = REGRA_DO_PRATO),
    )

    fun folga() = listOf(
        ItemPlano(tipoDia = "FOLGA", inicioMin = h(6), fimMin = h(7), tipo = TipoItem.TREINO_FOLGA,
            titulo = "Treino", descricao = "Academia",
            tituloAlt = "Caminhada", descricaoAlt = "Caminhada de 30 a 40 minutos"),
        ItemPlano(tipoDia = "FOLGA", inicioMin = h(7, 15), tipo = TipoItem.REFEICAO,
            titulo = "Café da manhã", descricao = CAFE),
        ItemPlano(tipoDia = "FOLGA", inicioMin = h(9), fimMin = h(11), tipo = TipoItem.DESCANSO,
            titulo = "Sono", descricao = "Dormir das 09:00 às 11:00"),
        ItemPlano(tipoDia = "FOLGA", inicioMin = h(12), tipo = TipoItem.REFEICAO,
            titulo = "Almoço", descricao = ALMOCO, marmita = Marmita.ALMOCO),
        ItemPlano(tipoDia = "FOLGA", inicioMin = h(16, 30), tipo = TipoItem.REFEICAO,
            titulo = "Lanche", descricao = "Escolha uma opção + 1 banana com aveia",
            opcoes = LANCHE_OPCOES),
        ItemPlano(tipoDia = "FOLGA", inicioMin = h(20), tipo = TipoItem.REFEICAO,
            titulo = "Janta", descricao = "150 g de frango, peixe ou ovos, 150 g de batata-doce, legumes",
            marmita = Marmita.JANTA),
    )
}
