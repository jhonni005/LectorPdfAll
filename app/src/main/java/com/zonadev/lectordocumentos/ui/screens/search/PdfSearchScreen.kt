package com.zonadev.lectordocumentos.ui.screens.search

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.zonadev.lectordocumentos.data.model.PdfItem
// IMPORTANTE: Asegúrate de importar el ViewModel independiente
import kotlinx.coroutines.android.awaitFrame

@Composable
fun PdfSearchScreen(
    onBack: () -> Unit,
    onOpenPdf: (Uri) -> Unit,
    viewModel: PdfSearchViewModel = viewModel()
) {
    val searchText by viewModel.searchText.collectAsState()

    // Recolectamos los resultados paginados desde el ViewModel de búsqueda
    val pagedSearchResults = viewModel.searchResults.collectAsLazyPagingItems()

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-foco del teclado al entrar
    LaunchedEffect(Unit) {
        awaitFrame() // Pequeño delay para asegurar que la UI está lista
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Atrás: Solo navega. No necesitamos limpiar el texto manualmente
                // porque al salir, este ViewModel se destruye.
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                }

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { viewModel.onSearchTextChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Buscar PDF...") },
                    singleLine = true
                )
            }
        }
    ) { padding ->
        // Solo mostramos la lista si hay texto escrito
        if (searchText.text.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Estado 1: Cargando (Spinner)
                if (pagedSearchResults.loadState.refresh is LoadState.Loading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }

                // Estado 2: Sin resultados
                if (pagedSearchResults.loadState.refresh is LoadState.NotLoading && pagedSearchResults.itemCount == 0) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No se encontraron resultados", color = Color.Gray)
                        }
                    }
                }

                // Estado 3: Lista de Resultados
                items(
                    count = pagedSearchResults.itemCount,
                    key = pagedSearchResults.itemKey { it.id },
                    contentType = { "pdf_search_row" }
                ) { index ->
                    val pdf = pagedSearchResults[index]
                    if (pdf != null) {
                        PdfSearchRow(
                            pdf = pdf,
                            query = searchText.text,
                            onClick = {
                                // Al abrir un PDF, no limpiamos nada.
                                // Si el usuario vuelve, el ViewModel sigue vivo y verá sus resultados.
                                onOpenPdf(pdf.uri)
                            }
                        )
                    }
                }

                // Estado 4: Cargando más resultados (Scroll infinito)
                if (pagedSearchResults.loadState.append is LoadState.Loading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        } else {
            // Pantalla inicial vacía
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Escribe para buscar...", color = Color.Gray)
            }
        }
    }
}

@Composable
fun PdfSearchRow(
    pdf: PdfItem,
    query: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        // Resaltamos el texto que coincide con la búsqueda
        Text(
            text = buildHighlightedString(pdf.name, query),
            style = MaterialTheme.typography.bodyLarge
        )
        // Puedes descomentar esto si quieres mostrar detalles (fecha/peso) en la búsqueda
        /*
        Text(
            text = pdf.details,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        */
    }
}

@Composable
fun buildHighlightedString(fullText: String, query: String): AnnotatedString {
    if (query.isBlank()) return androidx.compose.ui.text.AnnotatedString(fullText)

    return buildAnnotatedString {
        val lowerText = fullText.lowercase()
        val lowerQuery = query.lowercase()

        var startIndex = 0
        while (startIndex < fullText.length) {
            val index = lowerText.indexOf(lowerQuery, startIndex)
            if (index == -1) {
                append(fullText.substring(startIndex))
                break
            }
            // Parte normal
            append(fullText.substring(startIndex, index))
            // Parte resaltada (Rojo y Negrita)
            withStyle(style = SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold)) {
                append(fullText.substring(index, index + lowerQuery.length))
            }
            startIndex = index + lowerQuery.length
        }
    }
}