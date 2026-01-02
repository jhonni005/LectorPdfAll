package com.zonadev.lectordocumentos.ui.screens.home

import android.net.Uri
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.zonadev.lectordocumentos.core.ShareHelper
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.ui.components.*
import com.zonadev.lectordocumentos.ui.components.pdfoptions.PdfOptionsBottomSheet
import com.zonadev.lectordocumentos.ui.screens.home.PdfListScreen
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

/**
 * SavedPdfsScreen: Orquestador de la pestaña "Guardados" (Favoritos).
 * Gestiona la visualización de archivos almacenados en Room y sus acciones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfSavedScreen(
    onOpenPdf: (Uri) -> Unit,
    viewModel: PdfSavedViewModel
) {
    // --- 1. ESTADOS DEL VIEWMODEL Y PAGING ---
    val pagedPdfs = viewModel.pagedSavedPdfs.collectAsLazyPagingItems()
    val currentSortOption by viewModel.sortOption.collectAsState()
    val selectedPdf by viewModel.selectedPdfForOptions.collectAsState()
    val isCurrentlyFavorite by viewModel.isPdfFavorite.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState( initialFirstVisibleItemIndex = viewModel.lastScrollIndex ?: 0,
        initialFirstVisibleItemScrollOffset = viewModel.lastScrollOffset ?: 0)

    // Estados para el control de diálogos y hojas secundarias
    var pdfToRename by remember { mutableStateOf<PdfItem?>(null) }
    var pdfForDetails by remember { mutableStateOf<PdfItem?>(null) }
    var pdfToDelete by remember { mutableStateOf<PdfItem?>(null) }


    LaunchedEffect(currentSortOption) {
        // Al detectar cambio en la opción de orden, volvemos al inicio de la lista
        listState.scrollToItem(0)
    }


    // --- 2. LÓGICA DE PERSISTENCIA Y RESTAURACIÓN DE SCROLL ---
    // --- 2. LÓGICA DE PERSISTENCIA SEGURA ---
    fun saveScrollPosition() {
        // CRÍTICO: No guardar si estamos restaurando o si la lista está vacía/cargando.
        // Esto evita que se guarde "0" accidentalmente al volver a la pantalla.
        if (viewModel.isRestoringScroll) return
        if (pagedPdfs.itemCount > 0) {
            viewModel.lastScrollIndex = listState.firstVisibleItemIndex
            viewModel.lastScrollOffset = listState.firstVisibleItemScrollOffset
        }
    }


    //Al navegar entre taps
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
            viewModel.markAsOpened(pdf)
            onOpenPdf(pdf.uri)
            saveScrollPosition()

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
   /* if (showSortSheet) {
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
    }*/

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