package br.com.meushape

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.meushape.ui.screens.ComprasAba
import br.com.meushape.ui.screens.EmBreve
import br.com.meushape.ui.screens.HojeAba
import br.com.meushape.ui.theme.TemaMeuShape

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TemaMeuShape { Principal() } }
    }
}

private data class Aba(val titulo: String, val icone: ImageVector)

private val ABAS = listOf(
    Aba("Hoje", Icons.Filled.Home),
    Aba("Treinos", Icons.Filled.Star),
    Aba("Compras", Icons.Filled.ShoppingCart),
    Aba("Sono", Icons.Filled.DateRange),
    Aba("Progresso", Icons.Filled.Favorite),
)

@Composable
private fun Principal() {
    var aba by rememberSaveable { mutableIntStateOf(0) }

    // Pede permissão de notificação na primeira abertura (Android 13+).
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) pedir.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

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
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (aba) {
                0 -> HojeAba()
                1 -> EmBreve("Treinos", 3)
                2 -> ComprasAba()
                3 -> EmBreve("Sono", 4)
                else -> EmBreve("Progresso", 5)
            }
        }
    }
}
