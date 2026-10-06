package br.com.meushape.data

/** Listas de compras iniciais. Preços começam em zero: preencha no app. */
object ComprasPadrao {
    fun itens(): List<ItemCompra> {
        var ordem = 0
        fun s(nome: String, qtd: String, completo: Boolean = true, eco: Boolean = true, qtdEco: String = "") =
            ItemCompra(lista = Lista.SEMANAL, nome = nome, quantidade = qtd, noCompleto = completo,
                noEconomico = eco, quantidadeEco = qtdEco, ordem = ordem++)
        fun m(nome: String, qtd: String, eco: Boolean = true) =
            ItemCompra(lista = Lista.MENSAL, nome = nome, quantidade = qtd, noEconomico = eco, ordem = ordem++)
        return listOf(
            s("Ovos", "2 dúzias", qtdEco = "4 dúzias"),
            s("Peito de frango", "2 kg"),
            s("Pão integral", "1 pacote"),
            s("Bananas", "12"),
            s("Outra fruta", "7"),
            s("Alface", "1"),
            s("Tomates", "4"),
            s("Cenouras", "2"),
            s("Legume para a janta", "1"),
            s("Batata-doce", "600 g"),
            s("Iogurte natural", "7 potes", eco = false),
            m("Arroz", "1,2 kg"),
            m("Feijão", "1,2 kg"),
            m("Aveia", "500 g"),
            m("Whey", "900 g", eco = false),
            m("Atum ou sardinha em lata", "4"),
        )
    }
}
