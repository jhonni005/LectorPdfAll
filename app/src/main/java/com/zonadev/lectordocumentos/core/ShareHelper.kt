package com.zonadev.lectordocumentos.core

import android.content.Context
import android.content.Intent
import android.util.Log
import com.zonadev.lectordocumentos.data.model.PdfItem

object ShareHelper {
    fun sharePdf(context: Context, pdf: PdfItem) {
        try {
            // Creamos el Intent de tipo "Enviar"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf" // Importante: Mime Type

                // Pasamos la URI del archivo (content://...)
                putExtra(Intent.EXTRA_STREAM, pdf.uri)

                // Extras opcionales para mejorar la presentación
                putExtra(Intent.EXTRA_SUBJECT, pdf.name)
                putExtra(Intent.EXTRA_TEXT, "Te comparto este documento PDF: ${pdf.name}")

                // Permisos temporales para que la app receptora (Gmail, WhatsApp) pueda leerlo
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Lanzamos el selector de aplicaciones del sistema
            val chooser = Intent.createChooser(shareIntent, "Compartir PDF")
            context.startActivity(chooser)

        } catch (e: Exception) {
            Log.e("Error", "Ocurrió un error", e)
        }
    }
}