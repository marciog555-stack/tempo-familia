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

// ---------------- Etapa 3: treinos ----------------

object Grupo {
    val TODOS = listOf(
        "Peito", "Costas", "Ombros", "Bíceps", "Tríceps", "Quadríceps",
        "Posterior", "Glúteos", "Panturrilha", "Abdômen", "Cardio",
    )
}

/** Exercício da biblioteca. */
@Entity(tableName = "exercicio")
data class Exercicio(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val grupo: String,
    val personalizado: Boolean = false,
)

/** Treino montado (A, B, C...). */
@Entity(tableName = "treino")
data class Treino(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val observacao: String = "",
    val ordem: Int = 0,
)

/** Exercício dentro de um treino, com séries, repetições, carga e descanso. */
@Entity(tableName = "treino_exercicio")
data class TreinoExercicio(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val treinoId: Long,
    val exercicioId: Long,
    val ordem: Int = 0,
    val series: Int = 3,
    val repeticoes: String = "10",
    val carga: Double = 0.0,
    val descansoSeg: Int = 60,
)

/** Uma vez que o treino foi feito. */
@Entity(tableName = "sessao_treino")
data class SessaoTreino(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val treinoId: Long,
    val data: String,
    val inicio: Long,
    val fim: Long? = null,
)

/** Série feita durante uma sessão. */
@Entity(tableName = "serie_feita")
data class SerieFeita(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessaoId: Long,
    val exercicioId: Long,
    val numero: Int,
    val carga: Double,
    val repeticoes: Int,
    val feitaEm: Long,
)

/** Linha do histórico: maior carga de um exercício em cada sessão. */
data class PontoCarga(val data: String, val cargaMax: Double, val series: Int)

// ---------------- Etapa 4: sono e passos ----------------

object StatusSono {
    const val PENDENTE = "PENDENTE"
    const val CONFIRMADO = "CONFIRMADO"
    const val RECUSADO = "RECUSADO"
}

/** Registro de sono (detectado automaticamente ou manual). Horários em epoch ms. */
@Entity(tableName = "registro_sono")
data class RegistroSono(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inicio: Long,
    val fim: Long,
    val status: String,
    val manual: Boolean = false,
)

/** Passos de um dia (data do calendário). */
@Entity(tableName = "passos")
data class PassosDia(
    @PrimaryKey val data: String,
    val passos: Int,
)

// ---------------- Etapa 5: progresso ----------------

/** Peso em jejum de um dia. */
@Entity(tableName = "peso")
data class Peso(
    @PrimaryKey val data: String,
    val kg: Double,
)

/** Medida da cintura (semanal). */
@Entity(tableName = "cintura")
data class Cintura(
    @PrimaryKey val data: String,
    val cm: Double,
)

object Angulo {
    const val FRENTE = "FRENTE"
    const val LADO = "LADO"
    const val COSTAS = "COSTAS"
    val TODOS = listOf(FRENTE to "Frente", LADO to "Lado", COSTAS to "Costas")
}

/** Foto de progresso salva na memória interna do app. */
@Entity(tableName = "foto")
data class Foto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val data: String,
    val angulo: String,
    val arquivo: String,
)

// ---------------- Horário diferente só num dia ----------------

/** Muda o horário de um item da rotina só naquele dia (a rotina normal não muda). */
@Entity(tableName = "horario_dia", primaryKeys = ["data", "itemId"])
data class HorarioDia(
    val data: String,
    val itemId: Long,
    val inicioMin: Int,
    val fimMin: Int? = null,
)
