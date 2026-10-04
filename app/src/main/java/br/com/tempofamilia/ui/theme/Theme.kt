package br.com.tempofamilia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Verde = Color(0xFF2E7D6B)
val VerdeClaro = Color(0xFFB8E6D8)
val Laranja = Color(0xFFE08A2E)
val Vermelho = Color(0xFFC62828)

private val Claro = lightColorScheme(
    primary = Verde,
    onPrimary = Color.White,
    primaryContainer = VerdeClaro,
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Laranja,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB8),
    background = Color(0xFFF7FBF9),
    surface = Color(0xFFF7FBF9),
    error = Vermelho,
)

private val Escuro = darkColorScheme(
    primary = Color(0xFF8FD5C0),
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF1F5F51),
    onPrimaryContainer = VerdeClaro,
    secondary = Color(0xFFFFB86B),
    secondaryContainer = Color(0xFF6B3E00),
)

@Composable
fun TempoFamiliaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Escuro else Claro,
        content = content,
    )
}
