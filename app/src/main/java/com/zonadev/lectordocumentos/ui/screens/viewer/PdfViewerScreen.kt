package com.zonadev.lectordocumentos.ui.screens.viewer

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun PdfViewerScreen(
    pdfUri: Uri,
    onBack: () -> Unit,
    // Inyección automática del ViewModel corregido que creamos antes
    viewModel: PdfViewerViewModel = viewModel()
) {
    // 1. Cargar el PDF al iniciar la pantalla
    // El ViewModel se encarga de no recargarlo si ya existe (por rotación de pantalla)
    LaunchedEffect(pdfUri) {
        viewModel.loadPdf(pdfUri)
    }

    val state by viewModel.uiState.collectAsState()

    PdfViewerContent(
        isLoading = state.isLoading,
        error = state.error,
        pageCount = state.pageCount,
        onRenderPage = { index -> viewModel.renderPage(index) }, // Delegamos el renderizado
        onBack = onBack
    )
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PdfViewerContent(
    isLoading: Boolean,
    error: String?,
    pageCount: Int,
    onRenderPage: suspend (Int) -> Bitmap?,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visor de Documento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // Nota: Si Icons.AutoMirrored te da error en versiones viejas de Compose,
                        // usa Icons.Default.ArrowBack
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
               windowInsets = WindowInsets(0.dp)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.LightGray) // Fondo gris para contrastar con las hojas blancas
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                error != null -> {
                    Text(
                        text = "Error: $error",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    // Lista eficiente: solo renderiza las páginas visibles
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(pageCount) { index ->
                            PdfPageItem(index = index, onRenderPage = onRenderPage)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPageItem(
    index: Int,
    onRenderPage: suspend (Int) -> Bitmap?
) {
    // Estado local de cada página (si tiene su bitmap cargado o no)
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Efecto para cargar esta página específica
    LaunchedEffect(index) {
        isLoading = true
        // Llamada asíncrona al ViewModel (que revisará su caché LruCache)
        val result = onRenderPage(index)
        if (result != null) {
            bitmap = result
        }
        isLoading = false
    }

    // Diseño de la hoja
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .background(Color.White)
            .heightIn(min = 200.dp), // Altura mínima para evitar "saltos" en el scroll
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(32.dp))
        } else {
            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Página ${index + 1}",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            } ?: Text("Error al cargar página", modifier = Modifier.padding(16.dp))
        }
    }
}