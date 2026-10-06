package br.com.meushape.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.meushape.ui.theme.Cinza
import br.com.meushape.ui.theme.Limao

/**
 * Gráfico de linha simples. [pontos] = (rótulo do eixo X, valor).
 * [meta] desenha uma linha tracejada (ex.: peso meta).
 */
@Composable
fun GraficoLinha(
    pontos: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    meta: Double? = null,
    unidade: String = "",
    cor: Color = Limao,
) {
    if (pontos.isEmpty()) {
        Box(modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
            Text("Sem dados ainda", color = Cinza)
        }
        return
    }
    val valores = pontos.map { it.second } + listOfNotNull(meta)
    val min = valores.min()
    val max = valores.max()
    val folga = ((max - min) * 0.1).coerceAtLeast(0.5)
    val baixo = min - folga
    val alto = max + folga
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            Text("%.1f%s".format(alto, unidade), color = Cinza, fontSize = 11.sp)
        }
        Canvas(Modifier.fillMaxWidth().height(180.dp)) {
            fun y(v: Double) = (size.height * (1 - (v - baixo) / (alto - baixo))).toFloat()
            fun x(i: Int) = if (pontos.size == 1) size.width / 2 else size.width * i / (pontos.size - 1)
            meta?.let {
                drawLine(
                    Cinza, Offset(0f, y(it)), Offset(size.width, y(it)), strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
                )
            }
            val caminho = Path()
            pontos.forEachIndexed { i, p ->
                val o = Offset(x(i), y(p.second))
                if (i == 0) caminho.moveTo(o.x, o.y) else caminho.lineTo(o.x, o.y)
            }
            drawPath(caminho, cor, style = Stroke(width = 5f))
            pontos.forEachIndexed { i, p -> drawCircle(cor, radius = 7f, center = Offset(x(i), y(p.second))) }
        }
        Row(Modifier.fillMaxWidth()) {
            Text("%.1f%s".format(baixo, unidade), color = Cinza, fontSize = 11.sp, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth()) {
            Text(pontos.first().first, color = Cinza, fontSize = 11.sp, modifier = Modifier.weight(1f))
            if (pontos.size > 1) Text(pontos.last().first, color = Cinza, fontSize = 11.sp)
        }
    }
}
