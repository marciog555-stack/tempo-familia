package br.com.meushape

import android.app.Application
import br.com.meushape.data.Repo
import br.com.meushape.notify.Alarmes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Aplicativo: prepara dados iniciais e agenda as notificações. */
class MeuShapeApp : Application() {
    val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Alarmes.criarCanais(this)
        escopo.launch {
            Repo.get(this@MeuShapeApp).prepararPrimeiraVez()
            Alarmes.agendarProximo(this@MeuShapeApp)
            br.com.meushape.notify.ResumoSemanal.agendar(this@MeuShapeApp)
        }
    }
}
