package br.com.meushape.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Banco local (Room/SQLite). Nada sai do celular.
 * Ao mudar o esquema em etapas futuras, aumente a versão e use migrações para não perder dados.
 */
@Database(
    entities = [
        ItemPlano::class, Feito::class, TrocaDiaEntity::class, Agua::class, Config::class,
        ItemCompra::class, CompraMarcada::class, EstoqueMarmita::class, MarmitaMontada::class,
        Exercicio::class, Treino::class, TreinoExercicio::class, SessaoTreino::class, SerieFeita::class,
        RegistroSono::class, PassosDia::class,
        Peso::class, Cintura::class, Foto::class,
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
)
abstract class Banco : RoomDatabase() {
    abstract fun plano(): PlanoDao
    abstract fun feitos(): FeitoDao
    abstract fun escala(): EscalaDao
    abstract fun agua(): AguaDao
    abstract fun config(): ConfigDao
    abstract fun compras(): ComprasDao
    abstract fun marmitas(): MarmitaDao
    abstract fun treinos(): TreinoDao
    abstract fun sono(): SonoDao
    abstract fun progresso(): ProgressoDao

    companion object {
        @Volatile private var instancia: Banco? = null

        fun get(ctx: Context): Banco = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(ctx.applicationContext, Banco::class.java, "meu_shape.db")
                .build().also { instancia = it }
        }
    }
}
