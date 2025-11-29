package com.zonadev.lectordocumentos.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.zonadev.lectordocumentos.data.model.PdfItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class PdfRepository(private val context: Context) {

    fun listPdfs(): List<PdfItem> {
        val pdfList = mutableListOf<PdfItem>()

        // 1. OPTIMIZACIÓN: Creamos el formateador UNA sola vez fuera del bucle.
        // Crear SimpleDateFormat es costoso; hacerlo por cada fila mata el rendimiento.
        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))

        // 2. Consulta a MediaStore
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED, // Fecha de modificación
            MediaStore.Files.FileColumns.SIZE           // Tamaño en bytes
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("application/pdf")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        val queryUri = MediaStore.Files.getContentUri("external")

        try {
            context.contentResolver.query(
                queryUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val uri = ContentUris.withAppendedId(queryUri, id)

                    // Conversión de datos crudos
                    // DATE_MODIFIED viene en SEGUNDOS, Java usa MILISEGUNDOS -> x1000
                    val dateMillis = cursor.getLong(dateColumn) * 1000
                    val sizeBytes = cursor.getLong(sizeColumn)

                    // OPTIMIZACIÓN: Pre-calculamos los textos para la UI aquí mismo
                    val dateStr = try { dateFormatter.format(dateMillis) } catch (e: Exception) { "" }
                    val sizeStr = formatSize(sizeBytes)

                    pdfList.add(
                        PdfItem(
                            id = id,
                            name = name,
                            uri = uri,
                            lastModified = dateMillis, // Para ordenar
                            size = sizeBytes,          // Para ordenar
                            displayDate = dateStr,     // Para mostrar (String ya listo)
                            displaySize = sizeStr      // Para mostrar (String ya listo)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Fallback Manual (Solo si es necesario, ej. Huawei antiguo)
        if (pdfList.isEmpty()) {
            val targetFolders = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            )

            targetFolders.forEach { folder ->
                if (folder.exists() && folder.canRead()) {
                    // Limitamos profundidad para no congelar la app
                    folder.walk().maxDepth(3)
                        .filter { it.isFile && it.extension.equals("pdf", ignoreCase = true) }
                        .forEach { file ->
                            val dateMillis = file.lastModified()
                            val sizeBytes = file.length()

                            pdfList.add(
                                PdfItem(
                                    id = file.hashCode().toLong(),
                                    name = file.name,
                                    uri = Uri.fromFile(file),
                                    lastModified = dateMillis,
                                    size = sizeBytes,
                                    displayDate = dateFormatter.format(dateMillis),
                                    displaySize = formatSize(sizeBytes)
                                )
                            )
                        }
                }
            }
        }

        return pdfList
    }

    // Función pura y rápida para formatear bytes a KB/MB
    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 KB"

        val kb = bytes / 1024.0
        val mb = kb / 1024.0

        return when {
            mb >= 1.0 -> String.format("%.1f MB", mb)
            else -> String.format("%.0f KB", kb)
        }
    }
}