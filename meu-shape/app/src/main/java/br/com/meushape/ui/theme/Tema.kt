package br.com.meushape.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Limao = Color(0xFFB6F35B)
val Laranja = Color(0xFFFF9F43)
val Azul = Color(0xFF5DADE2)
val Roxo = Color(0xFFB39DDB)
val Vermelho = Color(0xFFEF5350)
val Cinza = Color(0xFF9AA0A6)
val Fundo = Color(0xFF101214)
val Cartao = Color(0xFF1C1F23)

private val Esquema = darkColorScheme(
    primary = Limao,
    onPrimary = Color(0xFF1A2600),
    primaryContainer = Color(0xFF2E3D10),
    onPrimaryContainer = Limao,
    secondary = Laranja,
    onSecondary = Color.Black,
    background = Fundo,
    surface = Fundo,
    surfaceVariant = Cartao,
    surfaceContainer = Cartao,
    surfaceContainerHigh = Color(0xFF24282D),
    error = Vermelho,
)

/** Tema sempre escuro. */
@Composable
fun TemaMeuShape(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Esquema, typography = Typography(), content = content)
}
