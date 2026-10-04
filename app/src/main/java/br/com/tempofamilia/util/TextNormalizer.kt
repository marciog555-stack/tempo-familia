package br.com.tempofamilia.util

import java.text.Normalizer

/** Normaliza texto para comparação: sem acentos, minúsculo, sem espaços nem pontuação. */
object TextNormalizer {
    fun normalize(texto: CharSequence): String {
        val semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
        val sb = StringBuilder(semAcento.length)
        for (c in semAcento) {
            // Letras e números ficam; acentos (marcas), espaços e pontuação saem.
            if (Character.isLetterOrDigit(c)) sb.append(c.lowercaseChar())
        }
        return sb.toString()
    }
}
