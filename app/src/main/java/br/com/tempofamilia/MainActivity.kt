package br.com.tempofamilia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.tempofamilia.data.Store
import br.com.tempofamilia.service.GuardService
import br.com.tempofamilia.ui.screens.AppsScreen
import br.com.tempofamilia.ui.screens.HomeScreen
import br.com.tempofamilia.ui.screens.SchedulesScreen
import br.com.tempofamilia.ui.screens.SettingsScreen
import br.com.tempofamilia.ui.screens.SetupScreen
import br.com.tempofamilia.ui.screens.WordsScreen
import br.com.tempofamilia.ui.theme.TempoFamiliaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        GuardService.start(this)
        setContent {
            TempoFamiliaTheme {
                Raiz()
            }
        }
    }
}

private data class Aba(val titulo: String, val icone: ImageVector)

private val ABAS = listOf(
    Aba("Início", Icons.Filled.Home),
    Aba("Apps", Icons.Filled.Star),
    Aba("Palavras", Icons.Filled.Lock),
    Aba("Horários", Icons.Filled.DateRange),
    Aba("Ajustes", Icons.Filled.Settings),
)

@Composable
private fun Raiz() {
    val estado by Store.state.collectAsState()
    when {
        !estado.loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        !estado.setupDone -> SetupScreen()
        else -> Principal()
    }
}

@Composable
private fun Principal() {
    var aba by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                ABAS.forEachIndexed { i, a ->
                    NavigationBarItem(
                        selected = aba == i,
                        onClick = { aba = i },
                        icon = { Icon(a.icone, contentDescription = a.titulo) },
                        label = { Text(a.titulo) },
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (aba) {
                0 -> HomeScreen(irParaApps = { aba = 1 }, irParaAjustes = { aba = 4 })
                1 -> AppsScreen()
                2 -> WordsScreen()
                3 -> SchedulesScreen()
                else -> SettingsScreen()
            }
        }
    }
}
