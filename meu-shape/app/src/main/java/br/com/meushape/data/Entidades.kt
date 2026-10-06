package br.com.meushape.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tipo de item da linha do tempo. */
object TipoItem {
    const val REFEICAO = "REFEICAO"
    const val TREINO = "TREINO"           // treino do plantão (todo plantão)
    const val TREINO_FOLGA = "TREINO_FOLGA" // treino ou caminhada (alterna nas folgas)
    const val DESCANSO = "DESCANSO"       // sono / cochilo
}

/** Qual estoque de marmita a refeição consome (usado na etapa 2). */
object Marmita {
    const val NENHUMA = ""
    const val ALMOCO = "ALMOCO"
    const val JANTA = "JANTA"
}

/** Item do cardápio/rotina de um tipo de dia (PLANTAO ou FOLGA). Tudo editável. */
@Entity(tableName = "item_plano")
data class ItemPlano(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipoDia: String,          // PLANTAO ou FOLGA
    val inicioMin: Int,           // minutos do dia lógico (pode passar de 1440)
    val fimMin: Int? = null,
    val tipo: String,
    val titulo: String,
    val descricao: String = "",
    /** Opções para escolher ao marcar (separadas por "|"), ex.: lanche do plantão. */
    val opcoes: String = "",
    /** Título/descrição alternativos usados na folga de caminhada. */
    val tituloAlt: String = "",
    val descricaoAlt: String = "",
    val marmita: String = Marmita.NENHUMA,
    val notificar: Boolean = true,
)

/** Item marcado como feito num dia. */
@Entity(tableName = "feito", primaryKeys = ["data", "itemId"])
data class Feito(
    val data: String,             // yyyy-MM-dd (dia lógico)
    val itemId: Long,
    val quando: Long,             // epoch ms
    val opcao: String = "",
    val refeicaoLivre: Boolean = false,
)

/** Troca manual do tipo do dia e/ou da atividade da folga. */
@Entity(tableName = "troca_dia")
data class TrocaDiaEntity(
    @PrimaryKey val data: String,
    val tipo: String? = null,
    val atividade: String? = null,
)

/** Copos de água por dia. */
@Entity(tableName = "agua")
data class Agua(
    @PrimaryKey val data: String,
    val copos: Int,
)

/** Configurações simples (chave/valor). */
@Entity(tableName = "config")
data class Config(
    @PrimaryKey val chave: String,
    val valor: String,
)

// ---------------- Etapa 2: compras e marmitas ----------------

object Lista {
    const val SEMANAL = "SEMANAL"
    const val MENSAL = "MENSAL"
}

/** Item da lista de compras. Preço é o valor total do item (ex.: R$ 18 pelas 2 dúzias). */
@Entity(tableName = "item_compra")
data class ItemCompra(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lista: String,
    val nome: String,
    val quantidade: String = "",
    val preco: Double = 0.0,
    /** Aparece na versão completa / econômica. */
    val noCompleto: Boolean = true,
    val noEconomico: Boolean = true,
    /** Quantidade e preço diferentes na versão econômica (vazio/nulo = iguais). */
    val quantidadeEco: String = "",
    val precoEco: Double? = null,
    val ordem: Int = 0,
)

/** Marcação de um item comprado num período (semana "2026-10-05" ou mês "2026-10"). */
@Entity(tableName = "compra_marcada", primaryKeys = ["itemId", "periodo"])
data class CompraMarcada(
    val itemId: Long,
    val periodo: String,
    val valor: Double = 0.0,
)

/** Estoque de marmitas por tipo (ALMOCO / JANTA). */
@Entity(tableName = "estoque_marmita")
data class EstoqueMarmita(
    @PrimaryKey val tipo: String,
    val geladeira: Int = 0,
    val freezer: Int = 0,
) {
    val total get() = geladeira + freezer
}

/** Registro de marmitas montadas (para a meta semanal). */
@Entity(tableName = "marmita_montada")
data class MarmitaMontada(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val data: String,
    val tipo: String,
    val quantidade: Int,
)
