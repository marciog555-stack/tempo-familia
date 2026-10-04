package br.com.tempofamilia.util

import br.com.tempofamilia.data.DefaultTerms
import br.com.tempofamilia.data.Store

/** Procura termos proibidos (lista padrão + termos do usuário) dentro de um texto. */
object KeywordMatcher {
    private val padrao: List<String> by lazy {
        DefaultTerms.ALL.map(TextNormalizer::normalize).filter { it.length >= 3 }.distinct()
    }

    @Volatile private var chaveCache: List<String>? = null
    @Volatile private var cache: List<String> = emptyList()

    private fun termos(): List<String> {
        val custom = Store.state.value.customTerms
        if (custom !== chaveCache) {
            cache = (padrao + custom.map(TextNormalizer::normalize))
                .filter { it.length >= 3 }
                .distinct()
            chaveCache = custom
        }
        return cache
    }

    /** Devolve o termo encontrado ou null. */
    fun find(texto: CharSequence): String? {
        val n = TextNormalizer.normalize(texto)
        if (n.length < 3) return null
        return termos().firstOrNull { n.contains(it) }
    }

    /** Quantidade de termos da lista padrão (para mostrar na tela). */
    val totalPadrao: Int get() = padrao.size
}
