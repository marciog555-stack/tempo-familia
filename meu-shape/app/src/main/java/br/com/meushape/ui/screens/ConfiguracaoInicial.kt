package br.com.meushape.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.meushape.data.Config
import br.com.meushape.data.Peso
import br.com.meushape.data.Repo
import br.com.meushape.logic.Dieta
import br.com.meushape.notify.Alarmes
import br.com.meushape.ui.CartaoApp
import br.com.meushape.ui.EscolherHora
import br.com.meushape.ui.theme.Cinza
import br.com.meushape.ui.theme.Limao
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private fun dec(t: String) = t.trim().replace(",", ".").toDoubleOrNull()
private val FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Configuração inicial para quem instala o app pela primeira vez. */
@Composable
fun ConfiguracaoInicial() {
    val ctx = LocalContext.current
    val repo = remember { Repo.get(ctx) }
    val scope = rememberCoroutineScope()
    var etapa by rememberSaveable { mutableIntStateOf(0) }

    // Sobre você
    var nome by rememberSaveable { mutableStateOf("") }
    var homem by rememberSaveable { mutableStateOf(true) }
    var idade by rememberSaveable { mutableStateOf("") }
    var altura by rememberSaveable { mutableStateOf("") }
    var peso by rememberSaveable { mutableStateOf("") }
    // Meta
    var meta by rememberSaveable { mutableStateOf("") }
    var dataMeta by rememberSaveable { mutableStateOf(LocalDate.now().plusMonths(4).toString()) }
    // Dieta
    var kcal by rememberSaveable { mutableStateOf("") }
    var prot by rememberSaveable { mutableStateOf("") }
    var carbo by rememberSaveable { mutableStateOf("") }
    var gord by rememberSaveable { mutableStateOf("") }
    // Escala
    var semEscala by rememberSaveable { mutableStateOf(true) }
    var diaPlantao by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var iniPlantao by rememberSaveable { mutableStateOf("19:00") }
    var fimPlantao by rememberSaveable { mutableStateOf("07:00") }

    var escolhendoData by remember { mutableStateOf<String?>(null) }
    var escolhendoHora by remember { mutableStateOf<String?>(null) }

    fun sugerir() {
        val m = Dieta.sugestao(homem, idade.toIntOrNull() ?: 30, dec(altura) ?: 170.0, dec(peso) ?: 75.0, dec(meta) ?: dec(peso) ?: 75.0)
        kcal = m.kcal.toString(); prot = m.proteina.toString(); carbo = m.carbo.toString(); gord = m.gordura.toString()
    }

    val total = 6
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Configuração inicial", color = Cinza)
        LinearProgressIndicator(progress = { (etapa + 1f) / total }, modifier = Modifier.fillMaxWidth())

        when (etapa) {
            0 -> {
                Text("Bem-vindo ao Meu Shape 💪", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Este app organiza sua dieta, treinos, sono, compras e progresso.\n\n" +
                        "Tudo fica salvo só no seu celular: sem login, sem internet, sem anúncios.\n\n" +
                        "Responda algumas perguntas para o app ficar do seu jeito. Dá para mudar tudo depois.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            1 -> {
                Titulo("Sobre você")
                OutlinedTextField(nome, { nome = it.take(30) }, label = { Text("Seu nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(homem, { homem = true }, { Text("Homem") })
                    FilterChip(!homem, { homem = false }, { Text("Mulher") })
                }
                Numero(idade, { idade = it }, "Idade (anos)")
                Numero(altura, { altura = it }, "Altura (cm), ex.: 175")
                Numero(peso, { peso = it }, "Peso atual (kg), ex.: 82,5")
            }
            2 -> {
                Titulo("Sua meta")
                Numero(meta, { meta = it }, "Peso que quer alcançar (kg)")
                OutlinedButton(onClick = { escolhendoData = "meta" }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Até: ${LocalDate.parse(dataMeta).format(FMT)}")
                }
                val p = dec(peso); val m = dec(meta)
                if (p != null && m != null) {
                    val semanas = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(dataMeta)) / 7.0
                    if (semanas > 0) {
                        val ritmo = (p - m) / semanas
                        Text(
                            "Ritmo: %.2f kg por semana".format(ritmo).replace(".", ",") +
                                if (ritmo > 1.0) " — muito rápido, considere uma data mais distante." else "",
                            color = if (ritmo > 1.0) br.com.meushape.ui.theme.Laranja else Cinza,
                        )
                    }
                }
            }
            3 -> {
                Titulo("Sua dieta")
                Text("Já sugerimos valores com base nos seus dados. Se tiver dieta de nutricionista, use os números dela.", color = Cinza)
                Numero(kcal, { kcal = it }, "Calorias por dia (kcal)")
                Numero(prot, { prot = it }, "Proteína (g)")
                Numero(carbo, { carbo = it }, "Carboidrato (g)")
                Numero(gord, { gord = it }, "Gordura (g)")
                TextButton(onClick = { sugerir() }) { Text("Recalcular sugestão") }
                Text("Sugestão é uma estimativa e não substitui um nutricionista.", color = Cinza, style = MaterialTheme.typography.bodySmall)
            }
            4 -> {
                Titulo("Sua escala de trabalho")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(semEscala, { semEscala = true }, { Text("Sem escala / horário comum") })
                    FilterChip(!semEscala, { semEscala = false }, { Text("Plantão 12x36") })
                }
                if (semEscala) {
                    Text("Todos os dias seguem a mesma rotina (treino e caminhada alternam).", color = Cinza)
                } else {
                    OutlinedButton(onClick = { escolhendoData = "plantao" }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text("Um dia de plantão: ${LocalDate.parse(diaPlantao).format(FMT)}")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { escolhendoHora = "ini" }, modifier = Modifier.weight(1f)) { Text("Início $iniPlantao") }
                        OutlinedButton(onClick = { escolhendoHora = "fim" }, modifier = Modifier.weight(1f)) { Text("Fim $fimPlantao") }
                    }
                    Text("O app calcula os plantões seguintes (um dia sim, um dia não).", color = Cinza)
                }
            }
            else -> {
                Titulo("Tudo pronto${if (nome.isNotBlank()) ", ${nome.trim()}" else ""}!")
                CartaoApp {
                    Text("${dec(peso) ?: "-"} kg → ${dec(meta) ?: "-"} kg até ${LocalDate.parse(dataMeta).format(FMT)}")
                    Text("Dieta: $kcal kcal · P $prot g · C $carbo g · G $gord g", color = Limao)
                    Text(if (semEscala) "Sem escala" else "Plantão 12x36, $iniPlantao às $fimPlantao", color = Cinza)
                }
                Text(
                    "Próximos passos:\n" +
                        "• Ajuste horários e refeições em Hoje → Editar rotina (já vem com um modelo).\n" +
                        "• Monte seus treinos ou use o treino de adaptação na aba Treinos.\n" +
                        "• Coloque os preços na aba Compras.\n" +
                        "• Na aba Sono, libere o funcionamento em segundo plano.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        val podeAvancar = when (etapa) {
            1 -> idade.toIntOrNull() != null && dec(altura) != null && dec(peso) != null
            2 -> dec(meta) != null
            3 -> listOf(kcal, prot, carbo, gord).all { it.toIntOrNull() != null }
            else -> true
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (etapa > 0) OutlinedButton(onClick = { etapa-- }, modifier = Modifier.weight(1f).height(56.dp)) { Text("Voltar") }
            Button(
                enabled = podeAvancar,
                modifier = Modifier.weight(1f).height(56.dp),
                onClick = {
                    if (etapa == 2 && kcal.isBlank()) sugerir()
                    if (etapa < total - 1) etapa++ else scope.launch {
                        val hoje = LocalDate.now()
                        val metaKg = dec(meta)!!
                        val cfg = mapOf(
                            "nome" to nome.trim(), "sexo" to if (homem) "Homem" else "Mulher",
                            "idade" to idade, "altura_cm" to (dec(altura)!!).toString(),
                            "peso_inicial" to (dec(peso)!!).toString(), "data_inicial" to hoje.toString(),
                            "meta_peso_min" to metaKg.toString(), "meta_peso_max" to metaKg.toString(),
                            "meta_data" to dataMeta, "kcal" to kcal, "proteina" to prot, "carbo" to carbo, "gordura" to gord,
                        )
                        cfg.forEach { (k, v) -> repo.db.config().salvar(Config(k, v)) }
                        repo.db.progresso().salvarPeso(Peso(hoje.toString(), dec(peso)!!))
                        repo.configurarEscala(semEscala, LocalDate.parse(diaPlantao))
                        repo.definirHorarioPlantao(LocalTime.parse(iniPlantao), LocalTime.parse(fimPlantao))
                        repo.db.config().salvar(Config("configuracao_inicial", "feita"))
                        Alarmes.agendarProximo(ctx)
                    }
                },
            ) { Text(if (etapa < total - 1) "Próximo" else "Começar") }
        }
    }

    escolhendoData?.let { qual ->
        EscolherSoData(
            inicial = LocalDate.parse(if (qual == "meta") dataMeta else diaPlantao),
            onDismiss = { escolhendoData = null },
        ) { d ->
            if (qual == "meta") dataMeta = d.toString() else diaPlantao = d.toString()
            escolhendoData = null
        }
    }
    escolhendoHora?.let { qual ->
        val atual = LocalTime.parse(if (qual == "ini") iniPlantao else fimPlantao)
        EscolherHora(atual.hour, atual.minute, onDismiss = { escolhendoHora = null }) { h, m ->
            val t = "%02d:%02d".format(h, m)
            if (qual == "ini") iniPlantao = t else fimPlantao = t
            escolhendoHora = null
        }
    }
}

@Composable
private fun Titulo(t: String) = Text(t, fontSize = 24.sp, fontWeight = FontWeight.Bold)

@Composable
private fun Numero(valor: String, onMudar: (String) -> Unit, rotulo: String) {
    OutlinedTextField(
        valor, { onMudar(it.take(7)) }, label = { Text(rotulo) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EscolherSoData(inicial: LocalDate, onDismiss: () -> Unit, onOk: (LocalDate) -> Unit) {
    val utc = inicial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val estado = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = utc)
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onOk(Instant.ofEpochMilli(estado.selectedDateMillis ?: utc).atZone(ZoneOffset.UTC).toLocalDate())
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    ) { androidx.compose.material3.DatePicker(state = estado) }
}
