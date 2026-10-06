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
