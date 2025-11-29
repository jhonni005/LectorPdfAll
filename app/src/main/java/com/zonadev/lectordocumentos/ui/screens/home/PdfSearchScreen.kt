package com.zonadev.lectordocumentos.ui.screens.home

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.zonadev.lectordocumentos.data.model.PdfItem
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay

@Composable
fun PdfSearchScreen(
    onBack: () -> Unit,
    onOpenPdf: (Uri) -> Unit,
    viewModel: PdfListViewModel = viewModel()
) {
    // Usamos TextFieldValue (que incluye posición del cursor)
    val searchText by viewModel.searchText.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        // Esperamos brevemente para asegurar que el TextField esté listo
        //delay(100)
        awaitFrame()
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Fondo y padding para evitar glitches con la barra de estado
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                }

                OutlinedTextField(
                    value = searchText, // Objeto completo con cursor
                    onValueChange = { viewModel.onSearchTextChange(it) }, // Actualiza estado completo
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(items = searchResults, key = { it.id }) { pdf ->
                PdfSearchRow(
                    pdf = pdf,
                    // Extraemos solo el texto String para la función de resaltado
                    query = searchText.text,
                    onClick = { onOpenPdf(pdf.uri) }
                )
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
            // Texto normal
            append(fullText.substring(startIndex, index))
            // Texto coincidente (Rojo)
            withStyle(style = SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold)) {
                append(fullText.substring(index, index + lowerQuery.length))
            }
            startIndex = index + lowerQuery.length
        }
    }
}