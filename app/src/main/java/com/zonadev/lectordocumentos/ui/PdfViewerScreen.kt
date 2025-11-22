package com.zonadev.lectordocumentos.ui


import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.runtime.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import androidx.core.graphics.createBitmap
import com.zonadev.lectordocumentos.utils.BitmapCache

import com.zonadev.lectordocumentos.utils.PdfRepository
import com.zonadev.lectordocumentos.utils.renderPdfPage
import io.legere.pdfiumandroid.PdfDocument
import io.legere.pdfiumandroid.PdfPage
import io.legere.pdfiumandroid.PdfiumCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import renderPdfPageWithPdfium


// --- 2. Pantalla Composable ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfiumViewerScreen(
    pdfUri: Uri,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    // Estados para Pdfium
    val pdfiumCoreState = remember { mutableStateOf<PdfiumCore?>(null) }
    val pdfDocState = remember { mutableStateOf<PdfDocument?>(null) }
    val totalPagesState = remember { mutableIntStateOf(0) }

    val mutex = remember { Mutex() }

    // Cache de memoria (igual que antes)
    val memoryCache = remember {
        object : LruCache<Int, Bitmap>(20 * 1024 * 1024) {
            override fun sizeOf(key: Int, value: Bitmap): Int = value.byteCount
        }
    }

    // Inicialización
    LaunchedEffect(pdfUri) {
        withContext(Dispatchers.IO) {
            try {
                // Copiamos el PDF a un archivo temporal porque Pdfium a veces prefiere File a Stream directo
                // O usamos ParcelFileDescriptor
                val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")

                if (pfd != null) {
                    val core = PdfiumCore(context)
                    val doc = core.newDocument(pfd) // Crear documento

                    pdfiumCoreState.value = core
                    pdfDocState.value = doc
                    totalPagesState.intValue = core.getPageCount(doc)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val core = pdfiumCoreState.value
    val doc = pdfDocState.value
    val totalPages = totalPagesState.intValue

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visor Pdfium") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        if (core == null || doc == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(totalPages, key = { it }) { pageIndex ->

                    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
                    var isLoading by remember { mutableStateOf(true) }

                    LaunchedEffect(pageIndex) {
                        val cached = memoryCache.get(pageIndex)
                        if (cached != null) {
                            bitmap = cached
                            isLoading = false
                        } else {
                            isLoading = true
                            // Renderizar con Pdfium
                            val bmp = renderPdfPageWithPdfium(core, doc, pageIndex, mutex)
                            if (bmp != null) {
                                memoryCache.put(pageIndex, bmp)
                                bitmap = bmp
                            }
                            isLoading = false
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator()
                        } else if (bitmap != null) {
                            Image(
                                bitmap = bitmap!!.asImageBitmap(),
                                contentDescription = "Página $pageIndex",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    // Limpieza de recursos
    DisposableEffect(Unit) {
        onDispose {
            try {
                val c = pdfiumCoreState.value
                val d = pdfDocState.value
                if (c != null && d != null) {
                    d.close() // Cerrar documento de Pdfium
                    // c.close() // PdfiumCore suele ser singleton o manejado por contexto, no siempre requiere close explícito, pero el doc sí.
                }
                memoryCache.evictAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}