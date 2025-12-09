package com.zonadev.lectordocumentos.ui.screens.home

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.zonadev.lectordocumentos.core.PermissionHelper
import com.zonadev.lectordocumentos.core.ShareHelper
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.data.model.PdfTab
import com.zonadev.lectordocumentos.ui.components.DeletePdfDialog
import com.zonadev.lectordocumentos.ui.components.PdfDetailsDialog
import com.zonadev.lectordocumentos.ui.components.pdfoptions.PdfOptionsBottomSheet
import com.zonadev.lectordocumentos.ui.components.PdfRow
import com.zonadev.lectordocumentos.ui.components.PermissionCard
import com.zonadev.lectordocumentos.ui.components.RenamePdfDialog
import com.zonadev.lectordocumentos.ui.components.SortBottomSheetContent
import com.zonadev.lectordocumentos.ui.utils.StableTopPadding
import kotlinx.coroutines.launch


// 1. NIVEL SUPERIOR: Lógica y Estado Global
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfAppEntry(
    onOpenPdf: (Uri) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: PdfListViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val currentSortOption by viewModel.sortOption.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val selectedPdf by viewModel.selectedPdfForOptions.collectAsState()

    // Recolectamos el flujo Paging 3 aquí
    val pagedPdfs = viewModel.pagedPdfList.collectAsLazyPagingItems()

    // Estados para Renombrar,Detalles y Eliminar
    var pdfToRename by remember { mutableStateOf<PdfItem?>(null) }
    var pdfForDetails by remember { mutableStateOf<PdfItem?>(null) }
    var pdfToDelete by remember { mutableStateOf<PdfItem?>(null) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Configuración del BottomSheet
    val sortSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortSheet by remember { mutableStateOf(false) }

    // Estado del scroll elevado
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    // Scroll al inicio al cambiar orden
    LaunchedEffect(currentSortOption) {
        listState.scrollToItem(0)
    }

    // Refrescar lista si cambian permisos
    LaunchedEffect(state.needsPermission) {
        if (!state.needsPermission) {
            pagedPdfs.refresh()
            viewModel.refresh()

        }
    }




    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true ||
                permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
        if (isGranted) {
            viewModel.refresh()
            pagedPdfs.refresh()
        }
    }

    // Detector de ciclo de vida
    DisposableEffect(lifecycleOwner) {
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
    }

    // Llamamos a la pantalla visual
    PdfListScreen(
        pagedPdfs = pagedPdfs,
        needsPermission = state.needsPermission,
        isPermissionSkipped = state.isPermissionSkipped,
        currentTab = currentTab,
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
        onOpenPdf = onOpenPdf,
        onSearchClick = onSearchClick,
        onSortClick = { showSortSheet = true },
        onMoreOptionsClick = { pdf ->
            viewModel.showPdfOptions(pdf)
        }
    )

    // --- AQUÍ LLAMAMOS A TU COMPONENTE ---

    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            sheetState = sortSheetState,
        ) {
            // Estado temporal para jugar con los checks sin aplicar cambios todavía
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
    // --- 3. CONECTAMOS EL NUEVO BOTTOM SHEET DE OPCIONES ---
    selectedPdf?.let{ pdf->
        PdfOptionsBottomSheet(
            pdf = selectedPdf!!,
            onDismiss = { viewModel.hidePdfOptions() },
            onShare = { ShareHelper.sharePdf(context,pdf) },
            onRename = {
                pdfToRename = selectedPdf
                viewModel.hidePdfOptions()
            },
            onDetails = {
                pdfForDetails = selectedPdf // Guardamos el PDF para ver sus detalles
                viewModel.hidePdfOptions()
            },
            onDelete = {
                pdfToDelete = pdf
                viewModel.hidePdfOptions()
                       },
            onToggleFavorite = { isFav -> /* Implementar logica favoritos */ }
        )
    }
    if (pdfToRename != null) {
        RenamePdfDialog(
            currentName = pdfToRename!!.name,
            onDismiss = { pdfToRename = null },
            onConfirm = { newName ->
                viewModel.renamePdf(pdfToRename!!, newName)
                pdfToRename = null
            }
        )
    }

    // 4. NUEVO: Diálogo de Detalles
    pdfForDetails?.let {details->
        PdfDetailsDialog(
            pdf = pdfForDetails!!,
            onDismiss = { pdfForDetails = null } // Limpiamos estado al cerrar
        )
    }

    pdfToDelete?.let {pdf->
        DeletePdfDialog(
            pdfName = pdf.name,
            onDismiss = {pdfToDelete = null},
            onConfirm = {
                viewModel.deletePdf(pdf)
                pdfToDelete = null
            }
        )
    }

}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfListScreen(
    pagedPdfs: LazyPagingItems<PdfItem>,
    needsPermission: Boolean,
    isPermissionSkipped: Boolean,
    currentTab: PdfTab,
    onRefresh: () -> Unit,
    onEnableManageAllFiles: () -> Unit,
    onSkipPermission: () -> Unit,
    onOpenPdf: (Uri) -> Unit,
    onSearchClick: () -> Unit,
    onSortClick: () -> Unit,
    onMoreOptionsClick: (PdfItem) -> Unit,
    listState: LazyListState
) {
    val titleText = if (currentTab == PdfTab.All) "Mis Documentos PDF" else currentTab.title

    // 1. Usamos el helper para obtener el padding estable
    val stableTopPadding = StableTopPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 2. TopAppBar
        TopAppBar(
            title = {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                )
            },
            actions = {
                IconButton(onClick = onSortClick) { Icon(Icons.AutoMirrored.Filled.Sort, "Ordenar") }
                IconButton(onClick = onSearchClick) { Icon(Icons.Default.Search, "Buscar") }
            },

            modifier = Modifier
                .fillMaxWidth()
                .padding(top = stableTopPadding),
            // 3. Clave: Desactivamos el padding interno de la barra
            windowInsets = WindowInsets(0.dp),

            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // 2. Contenido de la lista (Ocupa el resto del espacio)
        PdfListContent(
            modifier = Modifier.weight(1f),
            pagedPdfs = pagedPdfs,
            listState = listState,
            needsPermission = needsPermission,
            isPermissionSkipped = isPermissionSkipped,
            onRefresh = onRefresh,
            onEnableManageAllFiles = onEnableManageAllFiles,
            onSkipPermission = onSkipPermission,
            onOpenPdf = onOpenPdf,
            onMoreOptionsClick = onMoreOptionsClick
        )
    }
}









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
    onOpenPdf: (Uri) -> Unit,
    onMoreOptionsClick: (PdfItem) -> Unit
) {
    // Lógica anti-parpadeo para Huawei (Android < 12)
    val currentOverscroll = LocalOverscrollFactory.current
    val factoryToUse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) currentOverscroll else null

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
                // Solución de Scroll
                CompositionLocalProvider(
                    LocalOverscrollFactory provides factoryToUse,
                    LocalOverscrollConfiguration provides null
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        items(
                            count = pagedPdfs.itemCount,
                            key = pagedPdfs.itemKey { it.id },
                            contentType = { "pdf_row" }
                        ) { index ->
                            val pdf = pagedPdfs[index]
                            if (pdf != null) {
                                PdfRow(
                                    namePdf = pdf.name,
                                    pdfDetails = pdf.details,
                                    onClick = { onOpenPdf(pdf.uri) },
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
}