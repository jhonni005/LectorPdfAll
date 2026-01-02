package com.zonadev.lectordocumentos.ui.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfFormatUtils {

    /**
     * Convierte bytes a una cadena legible (KB o MB) con un decimal si es necesario.
     */
    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 KB"

        val kb = bytes / 1024.0
        val mb = kb / 1024.0

        return when {
            mb >= 1.0 -> String.format("%.1f MB", mb)
            else -> String.format("%.0f KB", kb)
        }
    }

    /**
     * Convierte un timestamp (milisegundos) a una fecha corta en español.
     * Ejemplo: "23 ago 2024"
     */
    fun formatDate(timestampMillis: Long): String {
        return try {
            // Es seguro crear SimpleDateFormat aquí porque no se comparte entre hilos de forma estática
            val date = Date(timestampMillis)
            val formatter = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
            formatter.format(date)
        } catch (e: Exception) {
            ""
        }
    }
}