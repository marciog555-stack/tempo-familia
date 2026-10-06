package br.com.meushape.data

import android.content.Context
import br.com.meushape.logic.AtividadeFolga
import br.com.meushape.logic.Escala
import br.com.meushape.logic.TipoDia
import br.com.meushape.logic.TrocaDia
import br.com.meushape.notify.Avisos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Ponto único de acesso aos dados. */
class Repo(private val ctx: Context) {
    val db = Banco.get(ctx)

    /** Cria o cardápio padrão e o perfil inicial na primeira vez. */
    suspend fun prepararPrimeiraVez() {
        if (db.plano().contar() == 0) {
            db.plano().inserir(PlanoPadrao.plantao() + PlanoPadrao.folga())
        }
        Perfil.PADRAO.forEach { (chave, valor) ->
            if (db.config().ler(chave) == null) db.config().salvar(Config(chave, valor))
        }
        // Listas de compras só são criadas uma vez (se você apagar tudo, não voltam).
        if (db.config().ler("compras_criadas") == null) {
            if (db.compras().contar() == 0) db.compras().inserir(ComprasPadrao.itens())
            db.config().salvar(Config("compras_criadas", "1"))
        }
        listOf(Marmita.ALMOCO, Marmita.JANTA).forEach {
            if (db.marmitas().estoque(it) == null) db.marmitas().salvarEstoque(EstoqueMarmita(it))
        }
    }

    // ---- Marcar refeições (com baixa automática de marmita) ----

    /** Marca um item como feito. Se a refeição usa marmita, tira 1 do estoque (geladeira primeiro). */
    suspend fun marcarFeito(dia: LocalDate, itemId: Long, opcao: String, livre: Boolean = false) {
        val ja = db.feitos().listarPeriodo(dia.toString(), dia.toString()).any { it.itemId == itemId }
        db.feitos().marcar(Feito(dia.toString(), itemId, System.currentTimeMillis(), opcao, livre))
        val item = db.plano().buscar(itemId) ?: return
        if (!ja && !livre && item.marmita.isNotEmpty()) {
            val e = db.marmitas().estoque(item.marmita) ?: EstoqueMarmita(item.marmita)
            val novo = when {
                e.geladeira > 0 -> e.copy(geladeira = e.geladeira - 1)
                e.freezer > 0 -> e.copy(freezer = e.freezer - 1)
                else -> e
            }
            db.marmitas().salvarEstoque(novo)
            if (novo != e && novo.total <= 2) Avisos.estoqueBaixo(ctx, novo)
        }
    }

    /** Desmarca e devolve a marmita para a geladeira, se tinha sido baixada. */
    suspend fun desmarcarFeito(dia: LocalDate, itemId: Long) {
        val feito = db.feitos().listarPeriodo(dia.toString(), dia.toString()).firstOrNull { it.itemId == itemId } ?: return
        db.feitos().desmarcar(dia.toString(), itemId)
        val item = db.plano().buscar(itemId) ?: return
        if (!feito.refeicaoLivre && item.marmita.isNotEmpty()) {
            val e = db.marmitas().estoque(item.marmita) ?: EstoqueMarmita(item.marmita)
            db.marmitas().salvarEstoque(e.copy(geladeira = e.geladeira + 1))
        }
    }

    // ---- Marmitas ----

    suspend fun montarMarmitas(tipo: String, qtd: Int, noFreezer: Boolean, dia: LocalDate) {
        val e = db.marmitas().estoque(tipo) ?: EstoqueMarmita(tipo)
        db.marmitas().salvarEstoque(
            if (noFreezer) e.copy(freezer = e.freezer + qtd) else e.copy(geladeira = e.geladeira + qtd)
        )
        db.marmitas().registrarMontada(MarmitaMontada(data = dia.toString(), tipo = tipo, quantidade = qtd))
    }

    suspend fun ajustarEstoque(e: EstoqueMarmita) {
        db.marmitas().salvarEstoque(e.copy(geladeira = e.geladeira.coerceAtLeast(0), freezer = e.freezer.coerceAtLeast(0)))
    }

    // ---- Compras ----

    suspend fun modoEconomico(): Boolean = db.config().ler("compras_economico") == "1"

    suspend fun definirModoEconomico(eco: Boolean) =
        db.config().salvar(Config("compras_economico", if (eco) "1" else "0"))

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
        /** Período de marcação: semana (segunda) para a lista semanal, mês para a mensal. */
        fun periodo(lista: String, dia: LocalDate): String =
            if (lista == Lista.SEMANAL) inicioSemana(dia).toString() else dia.toString().substring(0, 7)

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
