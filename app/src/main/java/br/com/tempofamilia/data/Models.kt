package br.com.tempofamilia.data

/** Um app com limite diário de uso. */
data class AppLimit(
    val packageName: String,
    val label: String,
    val minutesPerDay: Int,
)

/**
 * Faixa de horário da família (ex.: jantar). Dias seguem o padrão ISO:
 * 1 = segunda ... 7 = domingo. Horários em minutos desde a meia-noite.
 * Se o fim for menor que o início, a faixa atravessa a meia-noite.
 */
data class FamilySchedule(
    val id: Long,
    val name: String,
    val days: Set<Int>,
    val startMin: Int,
    val endMin: Int,
)

/** Todo o estado salvo do app. */
data class AppState(
    val loaded: Boolean = false,
    val setupDone: Boolean = false,
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val limits: List<AppLimit> = emptyList(),
    val customTerms: List<String> = emptyList(),
    val schedules: List<FamilySchedule> = emptyList(),
    val limitsEnabled: Boolean = true,
    val wordBlockEnabled: Boolean = true,
    val settingsGuardEnabled: Boolean = true,
    /** Até quando (epoch ms) as telas de configuração ficam liberadas após a senha. */
    val unlockedUntil: Long = 0L,
) {
    val hasPassword get() = passwordHash.isNotEmpty()
}
