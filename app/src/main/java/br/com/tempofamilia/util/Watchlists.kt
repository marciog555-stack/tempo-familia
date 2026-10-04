package br.com.tempofamilia.util

/** Listas de apps vigiados e regras de proteção das telas de Configurações. */
object Watchlists {

    /** Navegadores e apps de busca cujo conteúdo na tela é verificado. */
    val BROWSERS = setOf(
        "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "org.chromium.chrome",
        "com.sec.android.app.sbrowser", "com.sec.android.app.sbrowser.beta",
        "org.mozilla.firefox", "org.mozilla.firefox_beta", "org.mozilla.focus", "org.mozilla.fenix",
        "com.microsoft.emmx", "com.opera.browser", "com.opera.mini.native", "com.opera.gx",
        "com.brave.browser", "com.duckduckgo.mobile.android", "com.vivaldi.browser",
        "com.kiwibrowser.browser", "com.UCMobile.intl", "com.uc.browser.en", "mark.via.gp",
        "com.mi.globalbrowser", "com.yandex.browser", "com.android.browser", "com.ecosia.android",
        "com.google.android.googlequicksearchbox", "com.google.android.youtube",
        "com.google.android.apps.searchlite", "com.microsoft.bing", "com.qwant.liberty",
        "com.cloudmosa.puffinFree", "com.aloha.browser", "com.tor.browser", "org.torproject.torbrowser",
    )

    /** Pacotes das Configurações e do desinstalador (inclui os da Samsung e Realme/Oppo). */
    val SETTINGS_PACKAGES = setOf(
        "com.android.settings",
        // Realme / Oppo / OnePlus (ColorOS / Realme UI)
        "com.oplus.settings", "com.coloros.settings",
        "com.coloros.safecenter", "com.oplus.safecenter",
        "com.oplus.battery", "com.coloros.oppoguardelf",
        "com.oplus.packageinstaller", "com.coloros.packageinstaller",
        "com.oplus.securitypermission", "com.coloros.securitypermission",
        "com.samsung.android.settings",
        "com.samsung.accessibility",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
    )

    /** Pacotes que são inteiramente telas de Acessibilidade (Samsung). */
    val ACCESSIBILITY_PACKAGES = setOf("com.samsung.accessibility")

    private const val NOME_APP = "tempofamilia"

    /** Palavras que, junto do nome do app, indicam uma tela que pode desativá-lo. */
    private val ACOES_PERIGOSAS = listOf(
        "forcarparada", "forcarencerramento", "forcestop", "desinstalar", "uninstall",
        "admin", "acessibilidade", "accessibility", "acessoaouso", "acessoadadosdeuso",
        "dadosdeuso", "usageaccess", "sobrepor", "sobreposicao", "aparecersobre",
        "aparecernotopo", "displayover", "appearontop", "configuracoesrestritas",
        "restrictedsettings", "usartempofamilia", "inicializacaoautomatica",
        "autoinicializacao", "atividadeemsegundoplano", "usodabateria", "limpardados", "limpararmazenamento",
        "cleardata", "clearstorage", "desativar", "deactivate", "disable",
    )

    private val TELAS_SEMPRE_BLOQUEADAS = listOf(
        "dnsprivado", "privatedns",
        "administradoresdodispositivo", "appsdeadministracaododispositivo",
        "appsadmindodispositivo", "appsdeadmindodispositivo", "deviceadminapps",
        "appsdeadministradordodispositivo", "administradordodispositivo",
        "appsdeadministradordispositivo",
        "deviceadministrators",
    )

    /** Texto normalizado de uma tela de Configurações: ela deve ser bloqueada? */
    fun isProtectedSettingsScreen(textoNormalizado: String): Boolean {
        if (TELAS_SEMPRE_BLOQUEADAS.any { textoNormalizado.contains(it) }) return true
        if (textoNormalizado.contains(NOME_APP)) {
            return ACOES_PERIGOSAS.any { textoNormalizado.contains(it) }
        }
        return false
    }

    /** Nome da classe da janela indica Acessibilidade ou Administradores? */
    fun isProtectedSettingsClass(className: String): Boolean =
        className.contains("Accessibility", ignoreCase = true) ||
            className.contains("DeviceAdmin", ignoreCase = true) ||
            className.contains("PrivateDns", ignoreCase = true)
}
