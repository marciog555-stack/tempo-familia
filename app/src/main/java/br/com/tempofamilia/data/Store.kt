package br.com.tempofamilia.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tempo_familia")

/**
 * Armazenamento local (DataStore). Tudo fica só no celular.
 * O estado atual fica sempre disponível em [state] para a interface e os serviços.
 */
object Store {
    private lateinit var ds: DataStore<Preferences>
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state

    private val K_SETUP = booleanPreferencesKey("setup_done")
    private val K_HASH = stringPreferencesKey("senha_hash")
    private val K_SALT = stringPreferencesKey("senha_salt")
    private val K_LIMITS = stringPreferencesKey("limites")
    private val K_TERMS = stringPreferencesKey("termos")
    private val K_SCHED = stringPreferencesKey("horarios")
    private val K_LIM_ON = booleanPreferencesKey("limites_ativos")
    private val K_WORD_ON = booleanPreferencesKey("palavras_ativas")
    private val K_GUARD_ON = booleanPreferencesKey("protecao_config_ativa")
    private val K_UNLOCK = longPreferencesKey("liberado_ate")
    private val K_SEM_ON = booleanPreferencesKey("liberacao_semanal_ativa")
    private val K_SEM_ATIVADA = longPreferencesKey("liberacao_semanal_ativada_em")
    private val K_SEM_INICIO = longPreferencesKey("liberacao_semanal_inicio")
    private val K_SEM_USADAS = stringPreferencesKey("liberacao_semanal_usadas")

    fun init(context: Context) {
        if (::ds.isInitialized) return
        ds = context.applicationContext.dataStore
        scope.launch {
            ds.data.collect { _state.value = fromPrefs(it) }
        }
    }

    /** Altera o estado de forma atômica e salva. */
    fun update(transform: (AppState) -> AppState) {
        scope.launch {
            ds.edit { prefs ->
                val novo = transform(fromPrefs(prefs))
                toPrefs(novo, prefs)
            }
        }
    }

    private fun fromPrefs(p: Preferences) = AppState(
        loaded = true,
        setupDone = p[K_SETUP] ?: false,
        passwordHash = p[K_HASH] ?: "",
        passwordSalt = p[K_SALT] ?: "",
        limits = parseLimits(p[K_LIMITS]),
        customTerms = parseStrings(p[K_TERMS]),
        schedules = parseSchedules(p[K_SCHED]),
        limitsEnabled = p[K_LIM_ON] ?: true,
        wordBlockEnabled = p[K_WORD_ON] ?: true,
        settingsGuardEnabled = p[K_GUARD_ON] ?: true,
        unlockedUntil = p[K_UNLOCK] ?: 0L,
        weeklyReleaseEnabled = p[K_SEM_ON] ?: false,
        weeklyReleaseActivatedAt = p[K_SEM_ATIVADA] ?: 0L,
        weeklyReleaseStartedAt = p[K_SEM_INICIO] ?: 0L,
        weeklyReleaseUsedWeeks = parseStrings(p[K_SEM_USADAS]),
    )

    private fun toPrefs(s: AppState, p: MutablePreferences) {
        p[K_SETUP] = s.setupDone
        p[K_HASH] = s.passwordHash
        p[K_SALT] = s.passwordSalt
        p[K_LIMITS] = JSONArray().apply {
            s.limits.forEach {
                put(JSONObject().put("pkg", it.packageName).put("nome", it.label).put("min", it.minutesPerDay))
            }
        }.toString()
        p[K_TERMS] = JSONArray(s.customTerms).toString()
        p[K_SCHED] = JSONArray().apply {
            s.schedules.forEach {
                put(
                    JSONObject().put("id", it.id).put("nome", it.name)
                        .put("dias", JSONArray(it.days.sorted()))
                        .put("ini", it.startMin).put("fim", it.endMin)
                )
            }
        }.toString()
        p[K_LIM_ON] = s.limitsEnabled
        p[K_WORD_ON] = s.wordBlockEnabled
        p[K_GUARD_ON] = s.settingsGuardEnabled
        p[K_UNLOCK] = s.unlockedUntil
        p[K_SEM_ON] = s.weeklyReleaseEnabled
        p[K_SEM_ATIVADA] = s.weeklyReleaseActivatedAt
        p[K_SEM_INICIO] = s.weeklyReleaseStartedAt
        p[K_SEM_USADAS] = JSONArray(s.weeklyReleaseUsedWeeks).toString()
    }

    private fun parseLimits(json: String?): List<AppLimit> = runCatching {
        val arr = JSONArray(json ?: return emptyList())
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            AppLimit(o.getString("pkg"), o.getString("nome"), o.getInt("min"))
        }
    }.getOrDefault(emptyList())

    private fun parseStrings(json: String?): List<String> = runCatching {
        val arr = JSONArray(json ?: return emptyList())
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())

    private fun parseSchedules(json: String?): List<FamilySchedule> = runCatching {
        val arr = JSONArray(json ?: return emptyList())
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            val dias = o.getJSONArray("dias")
            FamilySchedule(
                id = o.getLong("id"),
                name = o.getString("nome"),
                days = (0 until dias.length()).map { i -> dias.getInt(i) }.toSet(),
                startMin = o.getInt("ini"),
                endMin = o.getInt("fim"),
            )
        }
    }.getOrDefault(emptyList())
}
