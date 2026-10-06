package br.com.meushape.data

import android.content.Context
import br.com.meushape.logic.AtividadeFolga
import br.com.meushape.logic.Escala
import br.com.meushape.logic.TipoDia
import br.com.meushape.logic.TrocaDia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Ponto único de acesso aos dados. */
class Repo(ctx: Context) {
    val db = Banco.get(ctx)

    /** Cria o cardápio padrão e o perfil inicial na primeira vez. */
    suspend fun prepararPrimeiraVez() {
        if (db.plano().contar() == 0) {
            db.plano().inserir(PlanoPadrao.plantao() + PlanoPadrao.folga())
        }
        Perfil.PADRAO.forEach { (chave, valor) ->
            if (db.config().ler(chave) == null) db.config().salvar(Config(chave, valor))
        }
    }

    // ---- Escala ----

    fun observarEscala(): Flow<Escala> = db.escala().observar().map { escalaDe(it) }

    suspend fun escala(): Escala = escalaDe(db.escala().listar())

    private fun escalaDe(lista: List<TrocaDiaEntity>) = Escala(
        trocas = lista.associate {
            LocalDate.parse(it.data) to TrocaDia(
                tipo = it.tipo?.let { t -> TipoDia.valueOf(t) },
                atividade = it.atividade?.let { a -> AtividadeFolga.valueOf(a) },
            )
        }
    )

    suspend fun trocarDia(data: LocalDate, tipo: TipoDia?, atividade: AtividadeFolga?) {
        if (tipo == null && atividade == null) db.escala().apagar(data.toString())
        else db.escala().salvar(TrocaDiaEntity(data.toString(), tipo?.name, atividade?.name))
    }

    // ---- Plano do dia ----

    fun observarPlano(tipo: TipoDia) = db.plano().observar(chavePlano(tipo))
    suspend fun plano(tipo: TipoDia) = db.plano().listar(chavePlano(tipo))

    // ---- Refeição livre: 1 por semana (segunda a domingo) ----

    fun observarFeitosDaSemana(dia: LocalDate): Flow<List<Feito>> {
        val seg = inicioSemana(dia)
        return db.feitos().observarPeriodo(seg.toString(), seg.plusDays(6).toString())
    }

    companion object {
        fun chavePlano(tipo: TipoDia) = if (tipo.usaCardapioFolga) "FOLGA" else "PLANTAO"
        fun inicioSemana(dia: LocalDate): LocalDate =
            dia.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))

        @Volatile private var instancia: Repo? = null
        fun get(ctx: Context): Repo = instancia ?: synchronized(this) {
            instancia ?: Repo(ctx.applicationContext).also { instancia = it }
        }
    }
}

/** Perfil e metas (editáveis na etapa 5, já salvos agora). */
object Perfil {
    val PADRAO = mapOf(
        "sexo" to "Homem",
        "idade" to "28",
        "altura_cm" to "168",
        "peso_inicial" to "74.2",
        "data_inicial" to "2026-10-06",
        "meta_peso_min" to "64",
        "meta_peso_max" to "65",
        "meta_data" to "2027-02-06",
        "kcal" to "1800",
        "proteina" to "150",
        "carbo" to "175",
        "gordura" to "55",
        "copo_ml" to "250",
        "meta_agua_ml" to "3000",
    )
}
