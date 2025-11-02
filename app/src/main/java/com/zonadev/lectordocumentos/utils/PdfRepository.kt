package com.zonadev.lectordocumentos.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.zonadev.lectordocumentos.model.PdfItem


class PdfRepository(private val context: Context) {

    fun listPdfs(): List<PdfItem> {
        val pdfList = mutableListOf<PdfItem>()

        try {
            // 📱 Primero intentamos usar MediaStore (funciona en Samsung, Pixel, etc.)
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME
            )

            val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
            val selectionArgs = arrayOf("application/pdf")

            val queryUri = MediaStore.Files.getContentUri("external")

            context.contentResolver.query(
                queryUri,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val uri = ContentUris.withAppendedId(queryUri, id)
                    pdfList.add(PdfItem(id, name, uri))
                }
            }
        } catch (_: Exception) {
            // ignoramos errores de MediaStore
        }

        // 📂 Si MediaStore no devolvió nada (ej. Huawei), hacemos búsqueda manual
        if (pdfList.isEmpty()) {
            val root = Environment.getExternalStorageDirectory()

            if (root.exists() && root.canRead()) {
                root.walkTopDown()
                    .filter { it.isFile && it.extension.equals("pdf", ignoreCase = true) }
                    .forEach { file ->
                        pdfList.add(
                            PdfItem(
                                id = file.hashCode().toLong(),
                                name = file.name,
                                uri = Uri.fromFile(file)
                            )
                        )
                    }
            }
        }

        return pdfList
    }

}
