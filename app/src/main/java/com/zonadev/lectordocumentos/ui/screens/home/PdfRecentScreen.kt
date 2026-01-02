package com.zonadev.lectordocumentos.ui.screens.home

import android.net.Uri
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.zonadev.lectordocumentos.core.ShareHelper
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.ui.components.DeletePdfDialog
import com.zonadev.lectordocumentos.ui.components.PdfDetailsDialog
import com.zonadev.lectordocumentos.ui.components.RenamePdfDialog
import com.zonadev.lectordocumentos.ui.components.SortBottomSheetContent
import com.zonadev.lectordocumentos.ui.components.pdfoptions.PdfOptionsBottomSheet
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfRecentScreen(
    onOpenPdf: (Uri) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: PdfRecentViewModel
) {
    // --- 1. ESTADOS DEL VIEWMODEL Y PAGING ---
    val pagedPdfs = viewModel.pagedRecentPdfs.collectAsLazyPagingItems()
    val currentSortOption by viewModel.sortOption.collectAsState()
    val selectedPdf by viewModel.selectedPdfForOptions.collectAsState()
    val isCurrentlyFavorite by viewModel.isPdfFavorite.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Estados para el control de diálogos y hojas secundarias
    var pdfToRename by remember { mutableStateOf<PdfItem?>(null) }
    var pdfForDetails by remember { mutableStateOf<PdfItem?>(null) }
    var pdfToDelete by remember { mutableStateOf<PdfItem?>(null) }

    val sortSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortSheet by remember { mutableStateOf(false) }

    // --- 2. LÓGICA DE PERSISTENCIA Y RESTAURACIÓN DE SCROLL ---
    fun saveScrollPosition() {
        // CRÍTICO: No guardar si estamos restaurando o si la lista está vacía/cargando.
        // Esto evita que se guarde "0" accidentalmente al volver a la pantalla.
        if (viewModel.isRestoringScroll) return
        if (pagedPdfs.itemCount > 0) {
            viewModel.lastScrollIndex = listState.firstVisibleItemIndex
            viewModel.lastScrollOffset = listState.firstVisibleItemScrollOffset
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            // Solo guardamos en onDispose si NO tenemos ya un valor pendiente (prioridad al click)
            if (viewModel.lastScrollIndex == null) {
                saveScrollPosition()
            }
        }
    }

//Recuepramos la posicion
    LaunchedEffect(pagedPdfs.itemCount) {
        val targetIndex = viewModel.lastScrollIndex
        val targetOffset = viewModel.lastScrollOffset ?: 0

        // Solo intentamos restaurar si tenemos un destino válido y datos suficientes
        if (targetIndex != null && pagedPdfs.itemCount > targetIndex) {
            viewModel.isRestoringScroll = true

            try {
                // INTENTO 1: Scroll inicial
                listState.scrollToItem(targetIndex, targetOffset)

                // Esperamos un frame para que Compose calcule el layout real
                awaitFrame()

                // INTENTO 2 (CORRECCIÓN): Verificamos si LazyColumn nos dejó donde queríamos.
                // Al final de la lista, a veces el primer scroll queda corto por falta de medición.
                // Si hay discrepancia, forzamos la corrección.
                if (listState.firstVisibleItemIndex != targetIndex ||
                    listState.firstVisibleItemScrollOffset != targetOffset) {
                    listState.scrollToItem(targetIndex, targetOffset)
                }
            } finally {
                // Liberamos el flag solo cuando estamos seguros
                viewModel.isRestoringScroll = false
                // Opcional: Limpiar el VM si quieres que el siguiente scroll sea libre
                viewModel.lastScrollIndex = null
                viewModel.lastScrollOffset = null
            }
        }
    }




    // --- 3. RENDERIZADO DE LA LISTA ---
    PdfListScreen(
        pagedPdfs = pagedPdfs,
        listState = listState,
        favoriteIds = favoriteIds,
        //  onSearchClick = onSearchClick,
        //   onSortClick = { showSortSheet = true },
        onOpenPdf = { pdf ->
            // Marcamos como abierto en recientes y guardamos scroll antes de navegar
         //   viewModel.markAsOpened(pdf)
            saveScrollPosition()
            onOpenPdf(pdf.uri)
        },
        onMoreOptionsClick = { pdf ->
            viewModel.showPdfOptions(pdf)
        },
        // En esta pestaña los permisos se manejan de forma pasiva o heredada
        needsPermission = false,
        isPermissionSkipped = false,
        onRefresh = { pagedPdfs.refresh() },
        onEnableManageAllFiles = {},
        onSkipPermission = {}
    )

    // --- 4. COMPONENTES DE INTERACCIÓN (MODALES) ---

    // Menú de Ordenamiento
    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            sheetState = sortSheetState,
        ) {
            var tempSelectedOption by remember { mutableStateOf(currentSortOption) }

            SortBottomSheetContent(
                selectedOption = tempSelectedOption,
                onOptionSelected = { tempSelectedOption = it },
                onCancel = {
                    scope.launch { sortSheetState.hide() }
                        .invokeOnCompletion { showSortSheet = false }
                },
                onApply = {
                    viewModel.updateSortOption(tempSelectedOption)
                    scope.launch { sortSheetState.hide() }
                        .invokeOnCompletion { showSortSheet = false }
                }
            )
        }
    }

    // Menú de Opciones del PDF seleccionado
    selectedPdf?.let { pdf ->
        PdfOptionsBottomSheet(
            pdf = pdf,
            isCurrentlyFavorite = isCurrentlyFavorite,
            onDismiss = { viewModel.hidePdfOptions() },
            onShare = { ShareHelper.sharePdf(context, pdf) },
            onRename = {
                pdfToRename = pdf
                viewModel.hidePdfOptions()
            },
            onDetails = {
                pdfForDetails = pdf
                viewModel.hidePdfOptions()
            },
            onDelete = {
                pdfToDelete = pdf
                viewModel.hidePdfOptions()
            },
            onToggleFavorite = { isFav ->
                viewModel.toggleFavorite(pdf, isFav)
            }
        )
    }

    // Diálogo: Renombrar
    pdfToRename?.let { pdf ->
        RenamePdfDialog(
            currentName = pdf.name,
            onDismiss = { pdfToRename = null },
            onConfirm = { newName ->
                viewModel.renamePdf(pdf, newName)
                pdfToRename = null
            }
        )
    }

    // Diálogo: Detalles
    pdfForDetails?.let { pdf ->
        PdfDetailsDialog(
            pdf = pdf,
            onDismiss = { pdfForDetails = null }
        )
    }

    // Diálogo: Borrar
    pdfToDelete?.let { pdf ->
        DeletePdfDialog(
            pdfName = pdf.name,
            onDismiss = { pdfToDelete = null },
            onConfirm = {
                viewModel.deletePdf(pdf)
                pdfToDelete = null
            }
        )
    }
}