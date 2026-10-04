package br.com.tempofamilia.data

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Senha da pessoa de confiança. Guardamos apenas o hash (PBKDF2 com salt aleatório),
 * nunca a senha em si.
 */
object Password {
    private const val ITERACOES = 120_000
    private const val TAMANHO_BITS = 256

    /** Tentativas erradas seguidas e bloqueio temporário contra "chutes". */
    private var erros = 0
    private var bloqueadoAte = 0L

    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hash(senha: String, salt: String): String {
        val spec = PBEKeySpec(senha.toCharArray(), Base64.decode(salt, Base64.NO_WRAP), ITERACOES, TAMANHO_BITS)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /** Segundos que faltam para poder tentar de novo (0 = pode tentar). */
    fun segundosDeEspera(): Long =
        ((bloqueadoAte - System.currentTimeMillis()) / 1000).coerceAtLeast(0)

    /** Confere a senha. Lento de propósito: chame fora da thread principal. */
    fun verify(senha: String): Boolean {
        if (segundosDeEspera() > 0) return false
        val s = Store.state.value
        if (!s.hasPassword) return true
        val ok = MessageDigest.isEqual(
            hash(senha, s.passwordSalt).toByteArray(),
            s.passwordHash.toByteArray()
        )
        if (ok) {
            erros = 0
        } else {
            erros++
            if (erros >= 5) {
                bloqueadoAte = System.currentTimeMillis() + 60_000L * (erros - 4)
            }
        }
        return ok
    }

    /** Define uma nova senha (gera novo salt). */
    fun setNew(senha: String) {
        val salt = newSalt()
        val h = hash(senha, salt)
        Store.update { it.copy(passwordHash = h, passwordSalt = salt) }
    }
}
