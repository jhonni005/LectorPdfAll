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
import java.io.IOException
import kotlin.math.min
import androidx.core.graphics.createBitmap

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

    // Mutex para evitar colisiones en la librería nativa
    private val renderMutex = Mutex()

    // OPTIMIZACIÓN 1: Caché dinámico basado en la memoria real del dispositivo (1/8 de la RAM disponible)
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8

    private val memoryCache = object : LruCache<Int, Bitmap>(cacheSize) {
        override fun sizeOf(key: Int, value: Bitmap): Int {
            // El tamaño se mide en Kilobytes
            return value.byteCount / 1024
        }
    }

    // Variable para guardar el ancho de pantalla
    private var screenWidth: Int = 1080 // Valor por defecto

    fun updateScreenWidth(widthPx: Int) {
        this.screenWidth = widthPx
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

    // OPTIMIZACIÓN 2: Usar Dispatchers.Default para cálculos matemáticos (Renderizado)
    suspend fun renderPage(index: Int): Bitmap? = withContext(Dispatchers.Default) {
        // Verificar caché primero
        synchronized(memoryCache) {
            memoryCache.get(index)
        }?.let { return@withContext it }

        renderMutex.withLock {
            val core = pdfiumCore
            val doc = pdfDocument

            if (core == null || doc == null) {
                return@withLock null
            }

            try {
                // Abrir página
                core.openPage(doc, index)

                val widthPoint = core.getPageWidthPoint(doc, index)
                val heightPoint = core.getPageHeightPoint(doc, index)

                // OPTIMIZACIÓN 3: Resolución inteligente
                // Usamos el ancho de la pantalla del usuario.
                // Si la pantalla es muy pequeña, usamos el ancho; si es una tablet 4K, limitamos a 2048px para no explotar memoria.
                val safeTargetWidth = min(screenWidth, 2048)

                val scale = safeTargetWidth.toFloat() / widthPoint
                val targetHeight = (heightPoint * scale).toInt()

                // Configuración RGB_565 ahorra 50% de RAM comparado con ARGB_8888 sin perder mucha calidad visible
                val bitmap = createBitmap(safeTargetWidth, targetHeight, Bitmap.Config.RGB_565)

                core.renderPageBitmap(
                    doc, bitmap, index,
                    0, 0, safeTargetWidth, targetHeight,
                    true
                )

                // Guardar en caché
                synchronized(memoryCache) {
                    memoryCache.put(index, bitmap)
                }

                return@withLock bitmap

            } catch (e: Exception) {
                Log.e("PdfViewer", "Error renderizando pagina $index", e)
                return@withLock null
            }
            // Nota: No cerramos la página aquí explícitamente porque PdfiumAndroid gestiona mejor tener el doc abierto
            // y renderizar bajo demanda. Cerrar/Abrir página constantemente consume CPU.
        }
    }

    override fun onCleared() {
        super.onCleared()
        val coreToClose = pdfiumCore
        val docToClose = pdfDocument
        val fdToClose = fileDescriptor

        pdfiumCore = null
        pdfDocument = null
        fileDescriptor = null
        memoryCache.evictAll()

        Thread {
            try {
                if (coreToClose != null && docToClose != null) {
                    coreToClose.closeDocument(docToClose)
                }
                fdToClose?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}