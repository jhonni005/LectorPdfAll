package com.zonadev.lectordocumentos.data.repository

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.zonadev.lectordocumentos.data.datasource.PdfPagingSource
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.data.model.PdfSortOption
import kotlinx.coroutines.flow.Flow
import java.io.File


class PdfRepository(private val context: Context) {

    // Esta función crea el flujo de datos paginados.
    // Paging 3 se encarga de llamar a tu 'PdfPagingSource' automáticamente
    // para pedir más datos cuando el usuario hace scroll.
    fun getPdfPager(sortOption: PdfSortOption, query: String): Flow<PagingData<PdfItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 50,        // Carga bloques de 20 en 20 (Muy rápido y ligero en RAM)
                enablePlaceholders = false, // No mostramos "huecos" vacíos
                prefetchDistance = 12, // Empieza a cargar la siguiente página cuando falten 10 items
                initialLoadSize = 100,   // La primera carga también es pequeña para que la app abra al instante
                maxSize = 300

            ),
            pagingSourceFactory = {
                // Cada vez que la lista se invalida (nuevo filtro/orden),
                // se crea una nueva fuente de datos limpia.
                PdfPagingSource(context, sortOption, query)
            }
        ).flow
    }



    /*ESTABLE CON DELAY Y MUY RAPIDO
      config = PagingConfig(
                pageSize = 30,        // Carga bloques de 20 en 20 (Muy rápido y ligero en RAM)
                enablePlaceholders = false, // No mostramos "huecos" vacíos
                prefetchDistance = 5, // Empieza a cargar la siguiente página cuando falten 10 items
                initialLoadSize = 30,   // La primera carga también es pequeña para que la app abra al instante
                maxSize = 200

            ),
    **/


// --- 2. RENOMBRADO HÍBRIDO (La Solución Definitiva) ---

    // --- RENOMBRADO CON CALLBACK (Sin Delay) ---

    fun renamePdf(
        uri: Uri,
        newName: String,
        onScanCompleted: () -> Unit // <- Callback que se ejecutará al terminar
    ): Boolean {
        val finalName = if (newName.endsWith(".pdf", ignoreCase = true)) newName else "$newName.pdf"
        val realPath = getRealPathFromURI(uri)

        // 1. ESTRATEGIA FILE (Preferida)
        if (realPath != null) {
            val file = File(realPath)
            if (file.exists()) {
                return renameViaFile(file, finalName, onScanCompleted)
            }
        }

        // 2. ESTRATEGIA DOCUMENT FILE (Fallback)
        return renameViaDocumentFile(uri, finalName, onScanCompleted)
    }

    private fun renameViaFile(
        currentFile: File,
        newName: String,
        onScanCompleted: () -> Unit
    ): Boolean {
        return try {
            val newFile = File(currentFile.parent, newName)

            if (currentFile.renameTo(newFile)) {
                // A. Escaneamos el viejo para borrarlo del índice
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(currentFile.absolutePath),
                    null,
                    null
                )

                // B. Escaneamos el NUEVO y usamos el Callback
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(newFile.absolutePath), // Path nuevo
                    arrayOf("application/pdf"),    // MimeType
                ) { _, _ ->
                    // ESTO SE EJECUTA CUANDO ANDROID TERMINA DE ESCANEAR
                    onScanCompleted()
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun renameViaDocumentFile(
        uri: Uri,
        newName: String,
        onScanCompleted: () -> Unit
    ): Boolean {
        return try {
            val document = DocumentFile.fromSingleUri(context, uri) ?: return false

            if (document.canWrite() && document.renameTo(newName)) {
                val realPath = getRealPathFromURI(document.uri)
                if (realPath != null) {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(realPath),
                        arrayOf("application/pdf")
                    ) { _, _ -> onScanCompleted() }
                } else {
                    onScanCompleted()
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getRealPathFromURI(contentUri: Uri): String? {
        if (contentUri.scheme == "file") return contentUri.path
        return try {
            val proj = arrayOf(MediaStore.Files.FileColumns.DATA)
            context.contentResolver.query(contentUri, proj, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                    cursor.getString(index)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    //ELIMINACION DE UN PDF

    // --- 3. ELIMINACIÓN (NUEVO) ---

    fun deletePdf(uri: Uri): Boolean {
        // Estrategia A: Intentar borrar el archivo físico (Más rápido y limpio)
        val realPath = getRealPathFromURI(uri)
        if (realPath != null) {
            val file = File(realPath)
            if (file.exists()) {
                if (deleteViaFile(file)) return true
            }
        }

        // Estrategia B: Intentar borrar vía DocumentFile (SAF)
        return deleteViaDocumentFile(uri)
    }

    private fun deleteViaFile(file: File): Boolean {
        return try {
            if (file.delete()) {
                // CRÍTICO: Avisar al sistema que el archivo murió para que lo quite de la BD
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    null,
                    null
                )
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun deleteViaDocumentFile(uri: Uri): Boolean {
        return try {
            val document = DocumentFile.fromSingleUri(context, uri)
            // .delete() devuelve true si tuvo éxito
            document?.delete() == true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


}