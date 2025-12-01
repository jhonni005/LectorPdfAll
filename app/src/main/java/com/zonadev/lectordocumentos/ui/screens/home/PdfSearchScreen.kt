package com.zonadev.lectordocumentos.ui.screens.home

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay

@Composable
fun PdfSearchScreen(
    onBack: () -> Unit,
    onOpenPdf: (Uri) -> Unit,
    viewModel: PdfListViewModel = viewModel()
) {
    val searchText by viewModel.searchText.collectAsState()

    // --- CORRECCIÓN PAGING 3 ---
    // En lugar de 'searchResults', usamos el flujo paginado maestro.
    // Este flujo ya se filtra solo gracias al ViewModel que conecta el texto con la query SQL.
    val pagedSearchResults = viewModel.pagedPdfList.collectAsLazyPagingItems()

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        awaitFrame()
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
        // Solo mostramos resultados si el usuario ha escrito algo
        if (searchText.text.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Estado de Carga Inicial (Spinner al buscar)
                if (pagedSearchResults.loadState.refresh is LoadState.Loading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }

                // Mensaje si no hay resultados
                if (pagedSearchResults.loadState.refresh is LoadState.NotLoading && pagedSearchResults.itemCount == 0) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No se encontraron resultados", color = Color.Gray)
                        }
                    }
                }

                // Lista de Resultados Paginada
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
                            onClick = { onOpenPdf(pdf.uri) }
                        )
                    }
                }
            }
        } else {
            // Pantalla vacía inicial (opcional: mostrar historial o sugerencias)
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
        Text(
            text = buildHighlightedString(pdf.name, query),
            style = MaterialTheme.typography.bodyLarge
        )
     /*   Text(
            text = buildHighlightedString(pdf.details, query),
            style = MaterialTheme.typography.bodySmall
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = Color.LightGray.copy(alpha = 0.5f)
        )*/
        // Opcional: Mostrar detalles también en búsqueda
        // Text(text = pdf.details, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
fun buildHighlightedString(fullText: String, query: String): androidx.compose.ui.text.AnnotatedString {
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
            append(fullText.substring(startIndex, index))
            withStyle(style = SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold)) {
                append(fullText.substring(index, index + lowerQuery.length))
            }
            startIndex = index + lowerQuery.length
        }
    }
}