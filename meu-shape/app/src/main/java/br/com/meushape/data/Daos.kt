package br.com.meushape.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanoDao {
    @Query("SELECT * FROM item_plano WHERE tipoDia = :tipoDia ORDER BY inicioMin")
    fun observar(tipoDia: String): Flow<List<ItemPlano>>

    @Query("SELECT * FROM item_plano WHERE tipoDia = :tipoDia ORDER BY inicioMin")
    suspend fun listar(tipoDia: String): List<ItemPlano>

    @Query("SELECT * FROM item_plano WHERE id = :id")
    suspend fun buscar(id: Long): ItemPlano?

    @Query("SELECT COUNT(*) FROM item_plano")
    suspend fun contar(): Int

    @Insert
    suspend fun inserir(itens: List<ItemPlano>)

    @Upsert
    suspend fun salvar(item: ItemPlano)

    @Delete
    suspend fun apagar(item: ItemPlano)
}

@Dao
interface FeitoDao {
    @Query("SELECT * FROM feito WHERE data = :data")
    fun observarDia(data: String): Flow<List<Feito>>

    @Query("SELECT * FROM feito WHERE data BETWEEN :de AND :ate")
    fun observarPeriodo(de: String, ate: String): Flow<List<Feito>>

    @Query("SELECT * FROM feito WHERE data BETWEEN :de AND :ate")
    suspend fun listarPeriodo(de: String, ate: String): List<Feito>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun marcar(feito: Feito)

    @Query("DELETE FROM feito WHERE data = :data AND itemId = :itemId")
    suspend fun desmarcar(data: String, itemId: Long)
}

@Dao
interface EscalaDao {
    @Query("SELECT * FROM troca_dia")
    fun observar(): Flow<List<TrocaDiaEntity>>

    @Query("SELECT * FROM troca_dia")
    suspend fun listar(): List<TrocaDiaEntity>

    @Upsert
    suspend fun salvar(troca: TrocaDiaEntity)

    @Query("DELETE FROM troca_dia WHERE data = :data")
    suspend fun apagar(data: String)
}

@Dao
interface AguaDao {
    @Query("SELECT * FROM agua WHERE data = :data")
    fun observar(data: String): Flow<Agua?>

    @Query("SELECT * FROM agua WHERE data = :data")
    suspend fun buscar(data: String): Agua?

    @Upsert
    suspend fun salvar(agua: Agua)
}

@Dao
interface ConfigDao {
    @Query("SELECT * FROM config")
    fun observar(): Flow<List<Config>>

    @Query("SELECT valor FROM config WHERE chave = :chave")
    suspend fun ler(chave: String): String?

    @Upsert
    suspend fun salvar(config: Config)
}

@Dao
interface ComprasDao {
    @Query("SELECT * FROM item_compra ORDER BY lista, ordem, id")
    fun observarItens(): Flow<List<ItemCompra>>

    @Query("SELECT COUNT(*) FROM item_compra")
    suspend fun contar(): Int

    @Insert
    suspend fun inserir(itens: List<ItemCompra>)

    @Upsert
    suspend fun salvar(item: ItemCompra)

    @Delete
    suspend fun apagar(item: ItemCompra)

    @Query("SELECT * FROM compra_marcada WHERE periodo IN (:periodos)")
    fun observarMarcadas(periodos: List<String>): Flow<List<CompraMarcada>>

    @Query("SELECT * FROM compra_marcada WHERE periodo IN (:periodos)")
    suspend fun listarMarcadas(periodos: List<String>): List<CompraMarcada>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun marcar(m: CompraMarcada)

    @Query("DELETE FROM compra_marcada WHERE itemId = :itemId AND periodo = :periodo")
    suspend fun desmarcar(itemId: Long, periodo: String)
}

@Dao
interface MarmitaDao {
    @Query("SELECT * FROM estoque_marmita")
    fun observarEstoque(): Flow<List<EstoqueMarmita>>

    @Query("SELECT * FROM estoque_marmita WHERE tipo = :tipo")
    suspend fun estoque(tipo: String): EstoqueMarmita?

    @Upsert
    suspend fun salvarEstoque(e: EstoqueMarmita)

    @Insert
    suspend fun registrarMontada(m: MarmitaMontada)

    @Query("SELECT * FROM marmita_montada WHERE data BETWEEN :de AND :ate")
    fun observarMontadas(de: String, ate: String): Flow<List<MarmitaMontada>>
}

@Dao
interface TreinoDao {
    // Biblioteca
    @Query("SELECT * FROM exercicio ORDER BY grupo, nome")
    fun observarExercicios(): Flow<List<Exercicio>>

    @Query("SELECT COUNT(*) FROM exercicio")
    suspend fun contarExercicios(): Int

    @Insert
    suspend fun inserirExercicios(lista: List<Exercicio>)

    @Upsert
    suspend fun salvarExercicio(e: Exercicio): Long

    // Treinos
    @Query("SELECT * FROM treino ORDER BY ordem, nome")
    fun observarTreinos(): Flow<List<Treino>>

    @Query("SELECT * FROM treino WHERE id = :id")
    suspend fun treino(id: Long): Treino?

    @Upsert
    suspend fun salvarTreino(t: Treino): Long

    @Delete
    suspend fun apagarTreino(t: Treino)

    @Query("DELETE FROM treino_exercicio WHERE treinoId = :treinoId")
    suspend fun limparItens(treinoId: Long)

    @Query("SELECT * FROM treino_exercicio WHERE treinoId = :treinoId ORDER BY ordem")
    fun observarItens(treinoId: Long): Flow<List<TreinoExercicio>>

    @Query("SELECT * FROM treino_exercicio ORDER BY ordem")
    fun observarTodosItens(): Flow<List<TreinoExercicio>>

    @Query("SELECT * FROM treino_exercicio WHERE treinoId = :treinoId ORDER BY ordem")
    suspend fun itens(treinoId: Long): List<TreinoExercicio>

    @Upsert
    suspend fun salvarItem(i: TreinoExercicio)

    @Delete
    suspend fun apagarItem(i: TreinoExercicio)

    // Sessões
    @Insert
    suspend fun criarSessao(s: SessaoTreino): Long

    @Query("SELECT * FROM sessao_treino WHERE id = :id")
    fun observarSessao(id: Long): Flow<SessaoTreino?>

    @Query("SELECT * FROM sessao_treino WHERE fim IS NULL ORDER BY inicio DESC LIMIT 1")
    fun observarSessaoAberta(): Flow<SessaoTreino?>

    @Query("UPDATE sessao_treino SET fim = :fim WHERE id = :id")
    suspend fun encerrarSessao(id: Long, fim: Long)

    @Query("DELETE FROM sessao_treino WHERE id = :id")
    suspend fun apagarSessao(id: Long)

    @Query("SELECT * FROM sessao_treino WHERE inicio < :ate AND (fim IS NULL OR fim > :de)")
    suspend fun sessoesEntre(de: Long, ate: Long): List<SessaoTreino>

    @Query("SELECT * FROM sessao_treino WHERE fim IS NOT NULL AND data BETWEEN :de AND :ate")
    suspend fun sessoesNoPeriodo(de: String, ate: String): List<SessaoTreino>

    // Séries
    @Query("SELECT * FROM serie_feita WHERE sessaoId = :sessaoId")
    fun observarSeries(sessaoId: Long): Flow<List<SerieFeita>>

    @Insert
    suspend fun inserirSerie(s: SerieFeita)

    @Query("DELETE FROM serie_feita WHERE sessaoId = :sessaoId AND exercicioId = :exercicioId AND numero = :numero")
    suspend fun apagarSerie(sessaoId: Long, exercicioId: Long, numero: Int)

    @Query("DELETE FROM serie_feita WHERE sessaoId = :sessaoId")
    suspend fun apagarSeriesDaSessao(sessaoId: Long)

    /** Última carga usada em cada exercício (para sugerir na próxima vez). */
    @Query(
        "SELECT s.* FROM serie_feita s WHERE s.id IN " +
            "(SELECT MAX(id) FROM serie_feita GROUP BY exercicioId)"
    )
    suspend fun ultimasSeries(): List<SerieFeita>

    @Query(
        "SELECT t.data AS data, MAX(s.carga) AS cargaMax, COUNT(*) AS series FROM serie_feita s " +
            "JOIN sessao_treino t ON t.id = s.sessaoId WHERE s.exercicioId = :exercicioId " +
            "GROUP BY s.sessaoId ORDER BY t.inicio"
    )
    fun observarHistorico(exercicioId: Long): Flow<List<PontoCarga>>

    @Query("SELECT DISTINCT exercicioId FROM serie_feita")
    fun observarExerciciosComHistorico(): Flow<List<Long>>
}

@Dao
interface SonoDao {
    @Query("SELECT * FROM registro_sono WHERE fim >= :desde ORDER BY inicio DESC")
    fun observarDesde(desde: Long): Flow<List<RegistroSono>>

    @Query("SELECT * FROM registro_sono WHERE status = 'CONFIRMADO' AND fim >= :desde")
    fun observarConfirmados(desde: Long): Flow<List<RegistroSono>>

    @Query("SELECT * FROM registro_sono WHERE status = 'CONFIRMADO' AND fim >= :desde")
    suspend fun confirmados(desde: Long): List<RegistroSono>

    @Query("SELECT * FROM registro_sono WHERE status = 'PENDENTE' ORDER BY inicio")
    fun observarPendentes(): Flow<List<RegistroSono>>

    @Query("SELECT * FROM registro_sono WHERE fim > :de AND inicio < :ate")
    suspend fun sobrepostos(de: Long, ate: Long): List<RegistroSono>

    @Upsert
    suspend fun salvar(r: RegistroSono)

    @Insert
    suspend fun inserir(lista: List<RegistroSono>)

    @Delete
    suspend fun apagar(r: RegistroSono)

    @Query("SELECT * FROM passos WHERE data = :data")
    fun observarPassos(data: String): Flow<PassosDia?>

    @Query("SELECT * FROM passos WHERE data BETWEEN :de AND :ate")
    suspend fun passosNoPeriodo(de: String, ate: String): List<PassosDia>

    @Upsert
    suspend fun salvarPassos(p: PassosDia)
}

@Dao
interface ProgressoDao {
    @Query("SELECT * FROM peso ORDER BY data")
    fun observarPesos(): Flow<List<Peso>>

    @Query("SELECT * FROM peso WHERE data BETWEEN :de AND :ate")
    suspend fun pesosEntre(de: String, ate: String): List<Peso>

    @Upsert
    suspend fun salvarPeso(p: Peso)

    @Delete
    suspend fun apagarPeso(p: Peso)

    @Query("SELECT * FROM cintura ORDER BY data")
    fun observarCintura(): Flow<List<Cintura>>

    @Upsert
    suspend fun salvarCintura(c: Cintura)

    @Delete
    suspend fun apagarCintura(c: Cintura)

    @Query("SELECT * FROM foto ORDER BY data DESC, angulo")
    fun observarFotos(): Flow<List<Foto>>

    @Insert
    suspend fun inserirFoto(f: Foto)

    @Delete
    suspend fun apagarFoto(f: Foto)
}
