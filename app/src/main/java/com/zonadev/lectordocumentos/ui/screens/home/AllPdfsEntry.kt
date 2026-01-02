package com.zonadev.lectordocumentos.ui.screens.home

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.zonadev.lectordocumentos.core.PermissionHelper
import com.zonadev.lectordocumentos.core.ShareHelper
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.data.model.PdfTab
import com.zonadev.lectordocumentos.ui.components.*
import com.zonadev.lectordocumentos.ui.components.pdfoptions.PdfOptionsBottomSheet
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

/**
 * AllPdfsEntry: Orquestador visual y de lógica para la pestaña "Todos".
 * Maneja el estado de la lista paginada, diálogos de acción y persistencia de scroll.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPdfsEntry(
    onOpenPdf: (Uri) -> Unit,
    viewModel: AllPdfsViewModel = viewModel(),
) {
    // --- 1. ESTADOS DEL VIEWMODEL Y UI ---
    val state by viewModel.uiState.collectAsState()
    val currentSortOption by viewModel.sortOption.collectAsState()
    val selectedPdf by viewModel.selectedPdfForOptions.collectAsState()
    val pagedPdfs = viewModel.pagedPdfList.collectAsLazyPagingItems()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    // Estados locales para el control de diálogos
    var pdfToRename by remember { mutableStateOf<PdfItem?>(null) }
    var pdfForDetails by remember { mutableStateOf<PdfItem?>(null) }
    var pdfToDelete by remember { mutableStateOf<PdfItem?>(null) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Configuración del BottomSheet de ordenamiento
    //val sortSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    //var showSortSheet by remember { mutableStateOf(false) }

    // --- 2. LÓGICA DE PERSISTENCIA Y RESTAURACIÓN DE SCROLL ---



    /* Guardado inical
   fun saveScrollPosition() {
        viewModel.lastScrollIndex = listState.firstVisibleItemIndex
        viewModel.lastScrollOffset = listState.firstVisibleItemScrollOffset
    }
    DisposableEffect(Unit) {
        onDispose {
            saveScrollPosition()
        }
    }
    LaunchedEffect(pagedPdfs.loadState.refresh) {
        if (pagedPdfs.loadState.refresh is LoadState.NotLoading && viewModel.lastScrollIndex != null) {
            // Se activa el flag de restauración para evitar saltos innecesarios
            viewModel.isRestoringScroll = true
            withFrameNanos { } // Esperamos un frame para que el layout se asiente
            runCatching {
                listState.scrollToItem(
                    index = viewModel.lastScrollIndex!!,
                   scrollOffset = viewModel.lastScrollOffset ?: 0
                )
            }
            // Limpieza de estados de restauración
            viewModel.lastScrollIndex = null
            viewModel.lastScrollOffset = null
            viewModel.isRestoringScroll = false
        }
    }
*/

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

    DisposableEffect(Unit) {
        onDispose {
            // Solo guardamos en onDispose si NO tenemos ya un valor pendiente (prioridad al click)
            if (viewModel.lastScrollIndex == null) {
                saveScrollPosition()
            }
        }
    }

    // --- 3. RESTAURACIÓN DE SCROLL CON DOBLE VERIFICACIÓN ---
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







    LaunchedEffect(currentSortOption) {
        if (!viewModel.isRestoringScroll) {
            listState.scrollToItem(0)
        }
    }

    // --- 3. GESTIÓN DE PERMISOS Y CICLO DE VIDA ---
    /*
    LaunchedEffect(state.needsPermission) {
        if (!state.needsPermission) {
            pagedPdfs.refresh()
            viewModel.refresh()
        }
    }*/

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true ||
                permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
        if (isGranted) {
            viewModel.refresh()
           // pagedPdfs.refresh()
        }
    }

  /*  DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (PermissionHelper.hasManageAllFiles(context)) {
                    if (state.needsPermission) {
                        viewModel.refresh()
                        pagedPdfs.refresh()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }*/
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (PermissionHelper.hasManageAllFiles(context)) {
                    // [MODIFICADO] Condición estricta:
                    // Solo recargamos si el ViewModel cree que AUN necesitamos permiso (state.needsPermission).
                    // Si el usuario ya tiene permiso y solo cambia de tabs, esto es false y NO recarga.
                    if (state.needsPermission) {
                        viewModel.refresh()
                        // pagedPdfs.refresh() // Redundante
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }



    // --- 4. RENDERIZADO DE LA PANTALLA PRINCIPAL ---
    PdfListScreen(
        pagedPdfs = pagedPdfs,
        favoriteIds = favoriteIds,
        needsPermission = state.needsPermission,
        isPermissionSkipped = state.isPermissionSkipped,
        listState = listState,
        onRefresh = {
            viewModel.refresh()
            pagedPdfs.refresh()
        },
        onEnableManageAllFiles = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                viewModel.requestManageAllFiles(context)
            } else {
                legacyPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        },
        onSkipPermission = { viewModel.skipPermissionRequest() },
        onOpenPdf = { pdfItem ->
            viewModel.markPdfAsOpened(pdfItem)
            onOpenPdf(pdfItem.uri)
            saveScrollPosition()
        },
        onMoreOptionsClick = { pdf ->
            viewModel.showPdfOptions(pdf)
        }
    )

    // --- 5. COMPONENTES EMERGENTES (MODALES Y DIÁLOGOS) ---

    // Menú de Ordenamiento
 /*   if (showSortSheet) {
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

    // Menú de Opciones del PDF
    selectedPdf?.let { pdf ->
        PdfOptionsBottomSheet(
            pdf = pdf,
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
            isCurrentlyFavorite = favoriteIds.contains(pdf.id),
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

    // Diálogo: Confirmación de Eliminación
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