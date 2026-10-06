package br.com.meushape.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Banco local (Room/SQLite). Nada sai do celular.
 * Ao mudar o esquema em etapas futuras, aumente a versão e use migrações para não perder dados.
 */
@Database(
    entities = [ItemPlano::class, Feito::class, TrocaDiaEntity::class, Agua::class, Config::class],
    version = 1,
    exportSchema = true,
)
abstract class Banco : RoomDatabase() {
    abstract fun plano(): PlanoDao
    abstract fun feitos(): FeitoDao
    abstract fun escala(): EscalaDao
    abstract fun agua(): AguaDao
    abstract fun config(): ConfigDao

    companion object {
        @Volatile private var instancia: Banco? = null

        fun get(ctx: Context): Banco = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(ctx.applicationContext, Banco::class.java, "meu_shape.db")
                .build().also { instancia = it }
        }
    }
}
