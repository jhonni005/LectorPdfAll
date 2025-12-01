package com.zonadev.lectordocumentos.data.datasource

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.data.model.PdfSortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class PdfPagingSource(
    private val context: Context,
    private val sortOption: PdfSortOption,
    private val query: String
) : PagingSource<Int, PdfItem>() {

    override fun getRefreshKey(state: PagingState<Int, PdfItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PdfItem> {
        // Ejecutamos explícitamente en IO para liberar al hilo principal
        return withContext(Dispatchers.IO) {
            // 1. OPTIMIZACIÓN: Creamos el formateador AQUÍ (Local).
            // SimpleDateFormat no es thread-safe. Usarlo como variable de clase causa bloqueos en Paging 3.
            // Al ser local, cada hilo tiene el suyo y vuela.
            val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))

            try {
                val page = params.key ?: 0
                val pageSize = params.loadSize
                val offset = page * pageSize

                val pdfList = mutableListOf<PdfItem>()

                val projection = arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.DATE_MODIFIED,
                    MediaStore.Files.FileColumns.SIZE
                )

                var selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
                val selectionArgs = mutableListOf("application/pdf")

                if (query.isNotBlank()) {
                    selection += " AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
                    selectionArgs.add("%$query%")
                }

                val sortColumn = when (sortOption) {
                    PdfSortOption.DATE_DESC, PdfSortOption.DATE_ASC -> MediaStore.Files.FileColumns.DATE_MODIFIED
                    PdfSortOption.NAME_ASC, PdfSortOption.NAME_DESC -> MediaStore.Files.FileColumns.DISPLAY_NAME
                    PdfSortOption.SIZE_DESC, PdfSortOption.SIZE_ASC -> MediaStore.Files.FileColumns.SIZE
                }

                val sortDir = when (sortOption) {
                    PdfSortOption.DATE_DESC, PdfSortOption.NAME_DESC, PdfSortOption.SIZE_DESC -> "DESC"
                    else -> "ASC"
                }

                val sortOrderSql = "$sortColumn $sortDir"

                val queryUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Files.getContentUri("external")
                }

                // Query Paginada Optimizada
                val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val queryArgs = Bundle().apply {
                        putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                        putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs.toTypedArray())
                        putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrderSql)
                        putInt(ContentResolver.QUERY_ARG_LIMIT, pageSize)
                        putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                    }
                    context.contentResolver.query(queryUri, projection, queryArgs, null)
                } else {
                    val sortWithLimit = "$sortOrderSql LIMIT $pageSize OFFSET $offset"
                    context.contentResolver.query(
                        queryUri, projection, selection, selectionArgs.toTypedArray(), sortWithLimit
                    )
                }

                cursor?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val nameCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val dateCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                    val sizeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

                    while (c.moveToNext()) {
                        val id = c.getLong(idCol)
                        val name = c.getString(nameCol) ?: "Sin nombre"
                        val dateMillis = c.getLong(dateCol) * 1000
                        val sizeBytes = c.getLong(sizeCol)
                        val uri = ContentUris.withAppendedId(queryUri, id)

                        // Pre-cálculo seguro
                        val dateStr = try { dateFormatter.format(dateMillis) } catch (e: Exception) { "" }
                        val sizeStr = formatSize(sizeBytes)
                        val detailsFinal = "$dateStr - $sizeStr"

                        pdfList.add(
                            PdfItem(id, name, uri, dateMillis, sizeBytes, detailsFinal)
                        )
                    }
                }

                val nextKey = if (pdfList.size < pageSize) null else page + 1
                val prevKey = if (page == 0) null else page - 1

                LoadResult.Page(
                    data = pdfList,
                    prevKey = prevKey,
                    nextKey = nextKey
                )

            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }

    // Función estática simple, muy rápida
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