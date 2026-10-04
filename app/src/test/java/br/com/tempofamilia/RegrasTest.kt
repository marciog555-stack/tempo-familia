package br.com.tempofamilia

import br.com.tempofamilia.data.FamilySchedule
import br.com.tempofamilia.util.KeywordMatcher
import br.com.tempofamilia.util.Schedules
import br.com.tempofamilia.util.TextNormalizer
import br.com.tempofamilia.util.Watchlists
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class RegrasTest {

    @Test
    fun normalizaAcentosEspacosEMaiusculas() {
        assertEquals("pornografia", TextNormalizer.normalize("Pornô  Gra-fia"))
        assertEquals("tempofamilia", TextNormalizer.normalize("Tempo Família"))
    }

    @Test
    fun bloqueiaTermosComVariacoes() {
        assertNotNull(KeywordMatcher.find("vídeos   de SEXO grátis"))
        assertNotNull(KeywordMatcher.find("https://www.xvideos.com/"))
        assertNotNull(KeywordMatcher.find("P O R N"))
        assertNotNull(KeywordMatcher.find("pornhub"))
    }

    @Test
    fun naoBloqueiaTextosComuns() {
        val textos = listOf(
            "Análise de dados para computação na escola",
            "A reunião continua amanhã às 19h",
            "Sexo: masculino. Data de nascimento: 01/02/1990",
            "Batom nude da nova coleção",
            "Receita de bolo de cenoura com cobertura de chocolate",
            "Transação aprovada no cartão de crédito",
            "Essex e Sussex são condados da Inglaterra",
            "Campeonato Brasileiro: Flamengo x Palmeiras ao vivo",
            "Como ensinar matemática para crianças de 8 anos",
            "Previsão do tempo para São Paulo nesta semana",
            "Apelação no tribunal foi negada",
            "Bíblia Sagrada online - Salmos 23",
        )
        textos.forEach { assertNull("Falso positivo em: $it", KeywordMatcher.find(it)) }
    }

    @Test
    fun protegeTelasDeConfiguracao() {
        fun tela(t: String) = Watchlists.isProtectedSettingsScreen(TextNormalizer.normalize(t))
        assertTrue(tela("Tempo Família  Desinstalar  Forçar parada  Armazenamento"))
        assertTrue(tela("DNS privado  Automático  Desativado"))
        assertTrue(tela("Apps admin. do dispositivo  Tempo Família"))
        // Telas do Realme
        assertTrue(tela("Apps de administrador do dispositivo  Encontrar dispositivo"))
        assertTrue(tela("Tempo Família  Uso da bateria  Permitir atividade em segundo plano"))
        assertTrue(tela("Conexão e compartilhamento  DNS privado"))
        assertFalse(tela("Conexões  Sons e vibração  Notificações  Tela  Bateria"))
        assertFalse(tela("Apps  Tempo Família  WhatsApp  Instagram"))
    }

    @Test
    fun horariosDaFamilia() {
        // 2026-10-05 é segunda-feira
        val segunda19h30 = LocalDateTime.of(2026, 10, 5, 19, 30)
        val jantar = FamilySchedule(1, "Jantar", setOf(1, 2, 3, 4, 5), 19 * 60, 20 * 60 + 30)
        assertTrue(Schedules.isActive(jantar, segunda19h30))
        assertFalse(Schedules.isActive(jantar, segunda19h30.withHour(21)))
        assertFalse(Schedules.isActive(jantar, segunda19h30.minusDays(1))) // domingo

        // Atravessa a meia-noite: segunda 22:00 até 06:00
        val noite = FamilySchedule(2, "Noite", setOf(1), 22 * 60, 6 * 60)
        assertTrue(Schedules.isActive(noite, LocalDateTime.of(2026, 10, 5, 23, 0)))
        assertTrue(Schedules.isActive(noite, LocalDateTime.of(2026, 10, 6, 5, 0)))
        assertFalse(Schedules.isActive(noite, LocalDateTime.of(2026, 10, 6, 7, 0)))

        val prox = Schedules.next(listOf(jantar), LocalDateTime.of(2026, 10, 5, 21, 0))
        assertEquals(LocalDateTime.of(2026, 10, 6, 19, 0), prox?.second)
    }
}
