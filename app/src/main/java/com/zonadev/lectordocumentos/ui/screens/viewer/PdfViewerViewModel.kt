package com.zonadev.lectordocumentos.ui.screens.viewer

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.legere.pdfiumandroid.PdfDocument
import io.legere.pdfiumandroid.PdfiumCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PdfViewerViewModel(application: Application) : AndroidViewModel(application) {

    data class ViewerUiState(
        val isLoading: Boolean = true,
        val error: String? = null,
        val pageCount: Int = 0
    )

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    private var pdfiumCore: PdfiumCore? = null
    private var pdfDocument: PdfDocument? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    private val renderMutex = Mutex()

    private val memoryCache = object : LruCache<Int, Bitmap>(20 * 1024 * 1024) {
        override fun sizeOf(key: Int, value: Bitmap): Int = value.byteCount
    }

    fun loadPdf(uri: Uri) {
        if (pdfDocument != null) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext
                val fd = context.contentResolver.openFileDescriptor(uri, "r")

                if (fd == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "No se pudo leer el archivo")
                    return@launch
                }

                val core = PdfiumCore(context)
                val doc = core.newDocument(fd)

                fileDescriptor = fd
                pdfiumCore = core
                pdfDocument = doc

                val pages = core.getPageCount(doc)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    pageCount = pages,
                    error = null
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Error al abrir PDF: ${e.message}")
            }
        }
    }

    suspend fun renderPage(index: Int): Bitmap? = withContext(Dispatchers.IO) {
        memoryCache.get(index)?.let { return@withContext it }

        renderMutex.withLock {
            val core = pdfiumCore
            val doc = pdfDocument

            if (core == null || doc == null) {
                return@withLock null
            }

            try {
                core.openPage(doc, index)

                val widthPoint = core.getPageWidthPoint(doc, index)
                val heightPoint = core.getPageHeightPoint(doc, index)

                val targetWidth = 1080
                val scale = targetWidth.toFloat() / widthPoint
                val targetHeight = (heightPoint * scale).toInt()

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565)

                core.renderPageBitmap(
                    doc, bitmap, index,
                    0, 0, targetWidth, targetHeight,
                    true
                )

                memoryCache.put(index, bitmap)
                return@withLock bitmap

            } catch (e: Exception) {
                Log.e("PdfViewer", "Error renderizando pagina $index", e)
                return@withLock null
            }
        }
    }

    // --- CORRECCIÓN CLAVE: LIMPIEZA SIN BLOQUEAR LA UI ---
    override fun onCleared() {
        super.onCleared()

        // 1. Capturamos las referencias locales para usarlas en otro hilo
        val coreToClose = pdfiumCore
        val docToClose = pdfDocument
        val fdToClose = fileDescriptor

        // 2. Limpiamos las variables globales y la caché inmediatamente
        pdfiumCore = null
        pdfDocument = null
        fileDescriptor = null
        memoryCache.evictAll()

        // 3. Ejecutamos el cierre "pesado" (I/O) en un hilo separado
        // Usamos Thread simple porque el viewModelScope ya está cancelado en este punto
        Thread {
            try {
                if (coreToClose != null && docToClose != null) {
                    // Esta operación nativa es la que bloqueaba tu scroll
                    docToClose.close()
                }
                // Cerrar el archivo también es operación de disco
                fdToClose?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}