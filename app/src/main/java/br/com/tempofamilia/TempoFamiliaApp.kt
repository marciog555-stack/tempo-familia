package br.com.tempofamilia

import android.app.Application
import br.com.tempofamilia.data.Store

/** Classe do aplicativo: inicializa o armazenamento local assim que o processo nasce. */
class TempoFamiliaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
    }
}
