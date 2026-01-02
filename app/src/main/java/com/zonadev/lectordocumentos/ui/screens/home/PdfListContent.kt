package com.zonadev.lectordocumentos.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.ui.components.PdfRow
import com.zonadev.lectordocumentos.ui.components.PermissionCard


// 3. NIVEL INFERIOR (Contenido con solución de scroll)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PdfListContent(
    modifier: Modifier = Modifier,
    pagedPdfs: LazyPagingItems<PdfItem>,
    listState: LazyListState,
    needsPermission: Boolean,
    isPermissionSkipped: Boolean,
    onRefresh: () -> Unit,
    onEnableManageAllFiles: () -> Unit,
    onSkipPermission: () -> Unit,
    onOpenPdf: (PdfItem) -> Unit,
    onMoreOptionsClick: (PdfItem) -> Unit,
    favoriteIds: Set<Long>
) {


    Box(modifier = modifier.fillMaxSize()) {
        when {
            needsPermission && !isPermissionSkipped -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PermissionCard(onManageAllFiles = onEnableManageAllFiles, onSkip = onSkipPermission)
                }
            }
            needsPermission && isPermissionSkipped -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Sin permiso", color = MaterialTheme.colorScheme.error)
                        Button(onClick = onEnableManageAllFiles) { Text("Permitir") }
                    }
                }
            }
            pagedPdfs.loadState.refresh is LoadState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            pagedPdfs.loadState.refresh is LoadState.NotLoading && pagedPdfs.itemCount == 0 -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No se encontraron archivos PDF", textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onRefresh) { Text("Refrescar") }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        count = pagedPdfs.itemCount,
                        key = pagedPdfs.itemKey { it.id },
                        contentType = { "pdf_row" }
                    ) { index ->
                        val pdf = pagedPdfs[index]
                        if (pdf != null) {
                            val isFavorite = favoriteIds.contains(pdf.id)
                            PdfRow(
                                namePdf = pdf.name,
                                pdfDetails = pdf.details,
                                isFavorite = isFavorite,
                                onClick = { onOpenPdf(pdf) },
                                onMoreOptionsClick = { onMoreOptionsClick(pdf) }
                            )
                        }
                    }
                    if (pagedPdfs.loadState.append is LoadState.Loading) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}