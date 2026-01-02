package com.zonadev.lectordocumentos.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.zonadev.lectordocumentos.data.datasource.PdfPagingSource
import com.zonadev.lectordocumentos.data.local.PdfDatabase
import com.zonadev.lectordocumentos.data.mapper.toLocalPdfEntity
import com.zonadev.lectordocumentos.data.mapper.toPdfItem
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.data.model.PdfSortOption
import com.zonadev.lectordocumentos.data.model.PdfTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class PdfRepository(private val context: Context) {

    private val pdfDao = PdfDatabase.getDatabase(context).pdfDao()

    /**
     * Esta es la implementación correcta.
     * Separa MediaStore de Room para garantizar tipos seguros y reactividad.
     */
    fun getPdfPager(
        tab: PdfTab,
        sortOption: PdfSortOption,
        query: String
    ): Flow<PagingData<PdfItem>> {

        // 1. Caso Herramientas: Flow vacío
        if (tab == PdfTab.Tools) return flowOf(PagingData.empty())

        // 2. Casos de Room (Saved / Recent): Paginación directa desde BD
        if (tab != PdfTab.All) {
            return Pager(
                config = PagingConfig(
                    pageSize = 50,
                    enablePlaceholders = false,
                    prefetchDistance = 12
                ),
                pagingSourceFactory = {
                    when (tab) {
                        PdfTab.Saved -> when (sortOption) {
                            PdfSortOption.DATE_DESC -> pdfDao.getFavoritesByDateDesc()
                            PdfSortOption.DATE_ASC -> pdfDao.getFavoritesByDateAsc()
                            PdfSortOption.NAME_ASC -> pdfDao.getFavoritesByNameAsc()
                            PdfSortOption.NAME_DESC -> pdfDao.getFavoritesByNameDesc()
                            PdfSortOption.SIZE_ASC -> pdfDao.getFavoritesBySizeAsc()
                            PdfSortOption.SIZE_DESC -> pdfDao.getFavoritesBySizeDesc()
                        }

                        PdfTab.Recent -> pdfDao.getRecentsPaged()
                        else -> throw IllegalStateException("Tab no soportado")
                    }
                }
            ).flow.map { pagingData ->
                // IMPORTANTE: Mapeamos la entidad de Room al modelo de la UI
                pagingData.map { it.toPdfItem() }
            }
        }

        // 3. Caso Pestaña TODOS: Combinamos MediaStore
        return flow {

            val pagerFlow = Pager(
                config = PagingConfig(
                    pageSize = 50,
                    enablePlaceholders = false,
                    prefetchDistance = 12,
                    initialLoadSize = 100,
                    maxSize = 300
                ),
                pagingSourceFactory = {
                    // Pasamos los IDs reales para que el icono de favorito salga bien
                    PdfPagingSource(context, sortOption, query)
                }
            ).flow

            // 🔥 CORRECTO: Usamos emitAll para que el Flow del Pager siga vivo y reactivo
            emitAll(pagerFlow)
        }
    }


    fun getFavoriteIdsFlow(): Flow<Set<Long>> = pdfDao.getAllFavoriteIdsFlow()
        .map { list -> list.toSet() }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    suspend fun getFavoriteStatus(id: Long): Boolean = withContext(Dispatchers.IO) {
        pdfDao.getById(id)?.isFavorite ?: false
    }

    suspend fun toggleFavorite(pdf: PdfItem, isFavorrite: Boolean) {
        val existing = pdfDao.getById(pdf.id)

        if (existing != null) {
            pdfDao.updateFavorite(pdf.id, isFavorrite)
        } else {
            pdfDao.insertOrUpdate(
                pdf.toLocalPdfEntity(isFavorite = isFavorrite)
            )
        }
    }


    suspend fun markPdfAsOpened(pdf: PdfItem) {
        val now = System.currentTimeMillis()
        val existing = pdfDao.getById(pdf.id)

        if (existing != null) {
            pdfDao.updateLastOpened(pdf.id, now)
        } else {
            pdfDao.insertOrUpdate(
                pdf.toLocalPdfEntity(lastOpenedTime = now)
            )
        }
    }


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

    //Delete from Room
    suspend fun deleteFromRoom(id: Long) {
        pdfDao.deleteById(id)

    }


    // 🔥 NUEVO: Obtiene el ID del MediaStore (necesario para Room)
    private fun getMediaIdFromUri(contentUri: Uri): Long? {
        // Intenta obtener el ID directamente de la URI
        return try {
            ContentUris.parseId(contentUri)
        } catch (e: Exception) {
            null
        }
    }
}