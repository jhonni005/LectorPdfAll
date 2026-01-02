package com.zonadev.lectordocumentos.core

import android.app.Application
import android.content.Context
import android.widget.Toast
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.data.repository.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class PdfActions(private val repository: PdfRepository, private val app: Application) {

    suspend fun rename(pdf: PdfItem, newName: String, onSuccess: () -> Unit) {
        val success = withContext(Dispatchers.IO) {
            repository.renamePdf(
                uri = pdf.uri,
                newName = newName,
                onScanCompleted = onSuccess
            )
        }
        if (!success) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    app,
                    "No se pudo renombrar el archivo",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /** * Elimina el archivo del almacenamiento y limpia su registro en Room. */
    suspend fun delete(pdf: PdfItem, onSuccess: () -> Unit) {
        val success = withContext(Dispatchers.IO) { repository.deletePdf(pdf.uri) }
        if (success) {
            repository.deleteFromRoom(pdf.id)
            withContext(Dispatchers.Main) { onSuccess() }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    app,
                    "No se pudo eliminar el archivo",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /** * Actualiza el estado de favorito en la base de datos local. */
    suspend fun toggleFavorite(pdf: PdfItem, isFavorite: Boolean) {
        withContext(Dispatchers.IO) { repository.toggleFavorite(pdf, isFavorite) }
    }
}