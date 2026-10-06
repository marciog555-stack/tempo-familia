package br.com.meushape.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * Backup em JSON de todas as tabelas do banco (exceto fotos, que ficam só no aparelho).
 * Lê e grava as tabelas de forma genérica, então novas tabelas entram no backup sozinhas.
 */
object Backup {
    private val IGNORAR = setOf("room_master_table", "android_metadata", "sqlite_sequence", "foto")

    private fun tabelas(ctx: Context): List<String> {
        val db = Banco.get(ctx).openHelper.readableDatabase
        val lista = mutableListOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type='table'").use { c ->
            while (c.moveToNext()) lista += c.getString(0)
        }
        return lista.filter { it !in IGNORAR }
    }

    fun exportar(ctx: Context, destino: Uri) {
        val db = Banco.get(ctx).openHelper.readableDatabase
        val raiz = JSONObject()
            .put("app", "Meu Shape")
            .put("versao_banco", db.version)
            .put("criado_em", java.time.LocalDateTime.now().toString())
        val dados = JSONObject()
        for (t in tabelas(ctx)) {
            val linhas = JSONArray()
            db.query("SELECT * FROM `$t`").use { c ->
                while (c.moveToNext()) {
                    val o = JSONObject()
                    for (i in 0 until c.columnCount) {
                        val nome = c.getColumnName(i)
                        when (c.getType(i)) {
                            Cursor.FIELD_TYPE_NULL -> o.put(nome, JSONObject.NULL)
                            Cursor.FIELD_TYPE_INTEGER -> o.put(nome, c.getLong(i))
                            Cursor.FIELD_TYPE_FLOAT -> o.put(nome, c.getDouble(i))
                            else -> o.put(nome, c.getString(i))
                        }
                    }
                    linhas.put(o)
                }
            }
            dados.put(t, linhas)
        }
        raiz.put("dados", dados)
        ctx.contentResolver.openOutputStream(destino, "wt")!!.use { it.write(raiz.toString(1).toByteArray()) }
    }

    /** Substitui todos os dados pelos do arquivo. Devolve quantos registros foram importados. */
    fun importar(ctx: Context, origem: Uri): Int {
        val texto = ctx.contentResolver.openInputStream(origem)!!.use { it.readBytes().decodeToString() }
        val raiz = JSONObject(texto)
        require(raiz.optString("app") == "Meu Shape") { "Este arquivo não é um backup do Meu Shape." }
        val dados = raiz.getJSONObject("dados")
        val db = Banco.get(ctx).openHelper.writableDatabase
        val existentes = tabelas(ctx).toSet()
        var total = 0
        db.beginTransaction()
        try {
            for (t in existentes) db.execSQL("DELETE FROM `$t`")
            val nomes = dados.keys()
            while (nomes.hasNext()) {
                val t = nomes.next()
                if (t !in existentes) continue
                // Só colunas que existem na versão atual do banco.
                val colunas = mutableSetOf<String>()
                db.query("PRAGMA table_info(`$t`)").use { c -> while (c.moveToNext()) colunas += c.getString(1) }
                val linhas = dados.getJSONArray(t)
                for (i in 0 until linhas.length()) {
                    val o = linhas.getJSONObject(i)
                    val cv = ContentValues()
                    o.keys().forEach { k ->
                        if (k !in colunas) return@forEach
                        when (val v = o.get(k)) {
                            JSONObject.NULL -> cv.putNull(k)
                            is Int -> cv.put(k, v.toLong())
                            is Long -> cv.put(k, v)
                            is Double -> cv.put(k, v)
                            is Boolean -> cv.put(k, if (v) 1 else 0)
                            else -> cv.put(k, v.toString())
                        }
                    }
                    db.insert(t, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, cv)
                    total++
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        Banco.get(ctx).invalidationTracker.refreshVersionsAsync()
        return total
    }
}
