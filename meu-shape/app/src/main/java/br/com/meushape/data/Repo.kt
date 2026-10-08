package br.com.meushape.data

import android.content.Context
import br.com.meushape.logic.AtividadeFolga
import br.com.meushape.logic.Escala
import br.com.meushape.logic.Horario
import br.com.meushape.logic.Periodo
import br.com.meushape.logic.SonoCalc
import br.com.meushape.logic.TipoDia
import br.com.meushape.logic.TrocaDia
import br.com.meushape.notify.Avisos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
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
        if (db.config().ler("exercicios_criados") == null) {
            if (db.treinos().contarExercicios() == 0) db.treinos().inserirExercicios(ExerciciosPadrao.todos())
            db.config().salvar(Config("exercicios_criados", "1"))
        }
        listOf(Marmita.ALMOCO, Marmita.JANTA).forEach {
            if (db.marmitas().estoque(it) == null) db.marmitas().salvarEstoque(EstoqueMarmita(it))
        }
    }

    // ---- Marcar refeições (com baixa automática de marmita) ----

    /** A marmita só sai do estoque quando a refeição prevista foi feita (não livre, não pulada, não "outra coisa"). */
    private fun usouMarmita(f: Feito?, item: ItemPlano): Boolean =
        f != null && item.marmita.isNotEmpty() && !f.refeicaoLivre && !f.pulado && !f.opcao.startsWith(Feito.OUTRO)

    /**
     * Marca um item como feito (ou como "não fiz", com [pulado]).
     * Se a refeição usa marmita, tira 1 do estoque (geladeira primeiro); se mudar a marcação, devolve.
     */
    suspend fun marcarFeito(dia: LocalDate, itemId: Long, opcao: String, livre: Boolean = false, pulado: Boolean = false) {
        val antes = db.feitos().listarPeriodo(dia.toString(), dia.toString()).firstOrNull { it.itemId == itemId }
        val novo = Feito(dia.toString(), itemId, System.currentTimeMillis(), opcao, livre, pulado)
        db.feitos().marcar(novo)
        val item = db.plano().buscar(itemId)
        if (item != null) {
            val tinha = usouMarmita(antes, item)
            val tem = usouMarmita(novo, item)
            if (tem && !tinha) {
                val e = db.marmitas().estoque(item.marmita) ?: EstoqueMarmita(item.marmita)
                val depois = when {
                    e.geladeira > 0 -> e.copy(geladeira = e.geladeira - 1)
                    e.freezer > 0 -> e.copy(freezer = e.freezer - 1)
                    else -> e
                }
                db.marmitas().salvarEstoque(depois)
                if (depois != e && depois.total <= 2) Avisos.estoqueBaixo(ctx, depois)
            } else if (tinha && !tem) {
                devolverMarmita(item.marmita)
            }
        }
        br.com.meushape.notify.WidgetProvider.atualizar(ctx)
    }

    private suspend fun devolverMarmita(tipo: String) {
        val e = db.marmitas().estoque(tipo) ?: EstoqueMarmita(tipo)
        db.marmitas().salvarEstoque(e.copy(geladeira = e.geladeira + 1))
    }

    /** Desmarca e devolve a marmita para a geladeira, se tinha sido baixada. */
    suspend fun desmarcarFeito(dia: LocalDate, itemId: Long) {
        val feito = db.feitos().listarPeriodo(dia.toString(), dia.toString()).firstOrNull { it.itemId == itemId } ?: return
        db.feitos().desmarcar(dia.toString(), itemId)
        val item = db.plano().buscar(itemId) ?: return
        if (usouMarmita(feito, item)) devolverMarmita(item.marmita)
        br.com.meushape.notify.WidgetProvider.atualizar(ctx)
    }

    // ---- Treinos ----

    /** Começa uma sessão de treino (ou devolve a que já está aberta). */
    suspend fun comecarTreino(treinoId: Long): Long =
        db.treinos().criarSessao(
            SessaoTreino(treinoId = treinoId, data = Escala.diaLogico(java.time.LocalDateTime.now()).toString(),
                inicio = System.currentTimeMillis())
        )

    /** Encerra o treino e marca o item de treino do dia como feito na tela Hoje. */
    suspend fun encerrarTreino(sessao: SessaoTreino) {
        db.treinos().encerrarSessao(sessao.id, System.currentTimeMillis())
        val dia = LocalDate.parse(sessao.data)
        val escala = escala()
        val item = planoDoDia(dia, escala).firstOrNull {
            it.tipo == TipoItem.TREINO || it.tipo == TipoItem.TREINO_FOLGA
        } ?: return
        val ja = db.feitos().listarPeriodo(sessao.data, sessao.data).any { it.itemId == item.id }
        if (!ja) marcarFeito(dia, item.id, db.treinos().treino(sessao.treinoId)?.let { "Treino ${it.nome}" } ?: "")
    }

    /** Cria os treinos "Adaptação A" e "Adaptação B" prontos. */
    suspend fun criarTreinosAdaptacao() {
        val dao = db.treinos()
        val existentes = dao.observarExercicios().first().associateBy { it.nome }.toMutableMap()
        val treinos = dao.observarTreinos().first()
        listOf("Adaptação A" to TreinoAdaptacao.A, "Adaptação B" to TreinoAdaptacao.B).forEachIndexed { idx, (nome, linhas) ->
            if (treinos.any { it.nome == nome }) return@forEachIndexed
            val treinoId = dao.salvarTreino(
                Treino(nome = nome, observacao = "Fase de adaptação · corpo inteiro · carga leve", ordem = idx - 10)
            )
            linhas.forEachIndexed { ordem, l ->
                val exId = existentes[l.exercicio]?.id ?: dao.salvarExercicio(
                    Exercicio(nome = l.exercicio, grupo = l.grupo, personalizado = true)
                ).also { existentes[l.exercicio] = Exercicio(it, l.exercicio, l.grupo, true) }
                dao.salvarItem(
                    TreinoExercicio(treinoId = treinoId, exercicioId = exId, ordem = ordem,
                        series = l.series, repeticoes = l.reps, descansoSeg = l.descanso)
                )
            }
        }
    }

    suspend fun descartarTreino(sessaoId: Long) {
        db.treinos().apagarSeriesDaSessao(sessaoId)
        db.treinos().apagarSessao(sessaoId)
    }

    // ---- Sono ----

    suspend fun limiteSonoMin(): Int = db.config().ler("sono_limite_min")?.toIntOrNull() ?: 25

    /** Períodos que nunca contam como sono: plantão, treino (planejado e feito) e registros já existentes. */
    suspend fun exclusoesSono(de: LocalDateTime, ate: LocalDateTime): List<Periodo> {
        val escala = escala()
        val lista = escala.periodosDePlantao(de, ate).map { Periodo(it.first, it.second) }.toMutableList()
        var dia = Escala.diaLogico(de).minusDays(1)
        while (!dia.isAfter(ate.toLocalDate())) {
            planoDoDia(dia, escala).filter { it.tipo == TipoItem.TREINO || it.tipo == TipoItem.TREINO_FOLGA }.forEach {
                lista += Periodo(Horario.momento(dia, it.inicioMin), Horario.momento(dia, it.fimMin ?: (it.inicioMin + 60)))
            }
            dia = dia.plusDays(1)
        }
        val deMs = de.ms(); val ateMs = ate.ms()
        db.treinos().sessoesEntre(deMs, ateMs).forEach {
            lista += Periodo(it.inicio.ldt(), (it.fim ?: System.currentTimeMillis()).ldt())
        }
        db.sono().sobrepostos(deMs, ateMs).forEach { lista += Periodo(it.inicio.ldt(), it.fim.ldt()) }
        return lista
    }

    /** Chamado quando o telefone volta a ser usado: cria registros de sono pendentes de confirmação. */
    suspend fun processarVoltaDeUso(fimUltimoUso: Long, agora: Long) {
        val de = fimUltimoUso.ldt(); val ate = agora.ldt()
        val detectados = SonoCalc.detectar(de, ate, limiteSonoMin(), exclusoesSono(de, ate))
        if (detectados.isNotEmpty()) {
            db.sono().inserir(detectados.map { RegistroSono(inicio = it.ini.ms(), fim = it.fim.ms(), status = StatusSono.PENDENTE) })
        }
    }

    fun observarSonoConfirmado(): Flow<List<Periodo>> =
        db.sono().observarConfirmados(System.currentTimeMillis() - 8L * 24 * 3600 * 1000)
            .map { l -> l.map { Periodo(it.inicio.ldt(), it.fim.ldt()) } }

    // ---- Sequência de dias cumprindo dieta e treino ----

    /** Dia cumprido = todas as refeições marcadas (refeição livre vale) e o treino/caminhada feito. */
    suspend fun diaCumprido(dia: LocalDate, escala: Escala): Boolean {
        val itens = plano(escala.tipo(dia)).filter {
            it.tipo == TipoItem.REFEICAO || it.tipo == TipoItem.TREINO || it.tipo == TipoItem.TREINO_FOLGA
        }
        if (itens.isEmpty()) return false
        val feitos = db.feitos().listarPeriodo(dia.toString(), dia.toString()).filter { !it.pulado }.map { it.itemId }.toSet()
        return itens.all { it.id in feitos }
    }

    /** Dias seguidos cumpridos até ontem, mais hoje se hoje já estiver completo. */
    suspend fun sequencia(hoje: LocalDate): Int {
        val escala = escala()
        var n = if (diaCumprido(hoje, escala)) 1 else 0
        var d = hoje.minusDays(1)
        while (n < 1000 && diaCumprido(d, escala)) { n++; d = d.minusDays(1) }
        return n
    }

    // ---- Resumo semanal ----

    data class Resumo(
        val segunda: LocalDate,
        val pesoMedio: Double?,
        val treinos: Int,
        val refeicoesFeitas: Int,
        val refeicoesPlanejadas: Int,
        val sonoMedioMin: Long,
        val gastoCompras: Double,
        val passosMedios: Int,
    )

    suspend fun resumoSemana(segunda: LocalDate): Resumo {
        val domingo = segunda.plusDays(6)
        val pesos = db.progresso().pesosEntre(segunda.toString(), domingo.toString())
        val escala = escala()
        var treinos = 0; var refFeitas = 0; var refPlan = 0
        var d = segunda
        while (!d.isAfter(domingo)) {
            val itens = plano(escala.tipo(d))
            val feitos = db.feitos().listarPeriodo(d.toString(), d.toString()).filter { !it.pulado }.map { it.itemId }.toSet()
            itens.forEach {
                when (it.tipo) {
                    TipoItem.REFEICAO -> { refPlan++; if (it.id in feitos) refFeitas++ }
                    TipoItem.TREINO, TipoItem.TREINO_FOLGA -> if (it.id in feitos) treinos++
                }
            }
            d = d.plusDays(1)
        }
        val ini = segunda.atStartOfDay(); val fim = domingo.plusDays(1).atStartOfDay()
        val sono = db.sono().confirmados(ini.ms() - 86_400_000L).map { Periodo(it.inicio.ldt(), it.fim.ldt()) }
        val sonoMedio = SonoCalc.minutosNaJanela(sono, ini, fim) / 7
        val periodos = listOf(periodo(Lista.SEMANAL, segunda))
        val gasto = db.compras().listarMarcadas(periodos).sumOf { it.valor }
        val passos = db.sono().passosNoPeriodo(segunda.toString(), domingo.toString())
        return Resumo(
            segunda, pesos.takeIf { it.isNotEmpty() }?.map { it.kg }?.average(), treinos, refFeitas, refPlan,
            sonoMedio, gasto, if (passos.isEmpty()) 0 else passos.sumOf { it.passos } / 7,
        )
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

    fun observarEscala(): Flow<Escala> =
        combine(db.escala().observar(), db.config().observar()) { trocas, cfg ->
            val m = cfg.associate { it.chave to it.valor }
            escalaDe(trocas, m["escala_modo"], m["escala_referencia"])
        }

    suspend fun escala(): Escala =
        escalaDe(db.escala().listar(), db.config().ler("escala_modo"), db.config().ler("escala_referencia"))

    /** Liga/desliga a escala 12x36 e define o dia de plantão de referência. */
    suspend fun configurarEscala(semEscala: Boolean, referencia: LocalDate) {
        db.config().salvar(Config("escala_modo", if (semEscala) "SEM_ESCALA" else "12X36"))
        db.config().salvar(Config("escala_referencia", referencia.toString()))
    }

    private fun escalaDe(lista: List<TrocaDiaEntity>, modo: String?, referencia: String?) = Escala(
        referenciaPlantao = referencia?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: Escala.REFERENCIA_PLANTAO,
        semEscala = modo == "SEM_ESCALA",
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

    /** Rotina de um dia já com os horários alterados só para aquele dia, em ordem de horário. */
    suspend fun planoDoDia(dia: LocalDate, escala: Escala): List<ItemPlano> =
        aplicarHorarios(plano(escala.tipo(dia)), db.horarioDia().listarDia(dia.toString()))

    // ---- Refeição livre: 1 por semana (segunda a domingo) ----

    fun observarFeitosDaSemana(dia: LocalDate): Flow<List<Feito>> {
        val seg = inicioSemana(dia)
        return db.feitos().observarPeriodo(seg.toString(), seg.plusDays(6).toString())
    }

    companion object {
        /** Período de marcação: semana (segunda) para a lista semanal, mês para a mensal. */
        fun periodo(lista: String, dia: LocalDate): String =
            if (lista == Lista.SEMANAL) inicioSemana(dia).toString() else dia.toString().substring(0, 7)

        /** Aplica os horários alterados do dia sobre os itens da rotina. */
        fun aplicarHorarios(itens: List<ItemPlano>, ajustes: List<HorarioDia>): List<ItemPlano> {
            val porItem = ajustes.associateBy { it.itemId }
            return itens.map { i ->
                porItem[i.id]?.let { a -> i.copy(inicioMin = a.inicioMin, fimMin = a.fimMin) } ?: i
            }.sortedBy { it.inicioMin }
        }

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
        "sono_limite_min" to "25",
        "meta_passos" to "8000",
    )
}

/** Conversões entre epoch ms e data/hora local. */
fun LocalDateTime.ms(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
fun Long.ldt(): LocalDateTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(this), ZoneId.systemDefault())
