package com.zonadev.lectordocumentos.ui.screens.home

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.zonadev.lectordocumentos.core.PermissionHelper
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.data.model.PdfSortOption
import com.zonadev.lectordocumentos.ui.components.PdfRow
import com.zonadev.lectordocumentos.ui.components.PermissionCard
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

    // Recolectamos el flujo Paging 3 aquí
    val pagedPdfs = viewModel.pagedPdfList.collectAsLazyPagingItems()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Configuración del BottomSheet
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

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
        onSortClick = { showBottomSheet = true },
        listState = listState
    )

    // Bottom Sheet Global
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            var tempSelectedOption by remember { mutableStateOf(currentSortOption) }
            SortBottomSheetContent(
                selectedOption = tempSelectedOption,
                onOptionSelected = { tempSelectedOption = it },
                onCancel = { scope.launch { sheetState.hide() }.invokeOnCompletion { showBottomSheet = false } },
                onApply = {
                    viewModel.updateSortOption(tempSelectedOption)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showBottomSheet = false }
                }
            )
        }
    }
}

// 2. NIVEL MEDIO: Estructura Visual (Scaffold)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfListScreen(
    pagedPdfs: LazyPagingItems<PdfItem>,
    needsPermission: Boolean,
    isPermissionSkipped: Boolean,
    onRefresh: () -> Unit,
    onEnableManageAllFiles: () -> Unit,
    onSkipPermission: () -> Unit,
    onOpenPdf: (Uri) -> Unit,
    onSearchClick: () -> Unit,
    onSortClick: () -> Unit,
    listState: LazyListState
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Documentos PDF") },
                actions = {
                    IconButton(onClick = onSortClick) { Icon(Icons.AutoMirrored.Filled.Sort, "Ordenar") }
                    IconButton(onClick = onSearchClick) { Icon(Icons.Default.Search, "Buscar") }
                }
            )
        }
    ) { padding ->
        // 3. SEPARACIÓN DE CONTENIDO: Pasamos solo lo necesario
        PdfListContent(
            modifier = Modifier.padding(padding),
            pagedPdfs = pagedPdfs,
            listState = listState,
            needsPermission = needsPermission,
            isPermissionSkipped = isPermissionSkipped,
            onRefresh = onRefresh,
            onEnableManageAllFiles = onEnableManageAllFiles,
            onSkipPermission = onSkipPermission,
            onOpenPdf = onOpenPdf
        )
    }
}

// 3. NIVEL INFERIOR: Lista Pura (Aislada de recomposiciones)
// Este componente solo cambia si cambia la lista o el permiso, ignorando el BottomSheet
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
    onOpenPdf: (Uri) -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
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

            // Estado de Carga Inicial
            pagedPdfs.loadState.refresh is LoadState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }

            // Lista Vacía
            pagedPdfs.loadState.refresh is LoadState.NotLoading && pagedPdfs.itemCount == 0 -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No se encontraron archivos PDF")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onRefresh) { Text("Refrescar") }
                    }
                }
            }

            // LISTA DE DATOS (Zona Crítica)
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState,
                    // Evita cortes visuales al final de la lista
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        count = pagedPdfs.itemCount,
                        // Key estable para rendimiento en scroll
                        key = pagedPdfs.itemKey { it.id },
                        // ContentType para reciclaje de vistas eficiente
                        contentType = { "pdf_row" }
                    ) { index ->
                        val pdf = pagedPdfs[index]
                        if (pdf != null) {
                            // PdfRow optimizado (recibe strings)
                            PdfRow(
                                namePdf = pdf.name,
                                pdfDetails = pdf.details,
                                onClick = { onOpenPdf(pdf.uri) }
                            )
                        }
                    }

                    // Spinner al cargar más (scroll infinito)
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

// --- Componentes Auxiliares del Bottom Sheet ---

@Composable
fun SortBottomSheetContent(selectedOption: PdfSortOption, onOptionSelected: (PdfSortOption) -> Unit, onCancel: () -> Unit, onApply: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 24.dp)) {
        Text("Ordenar por", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        SortOptionRow("Última modificación (Reciente)", Icons.Default.History, selectedOption == PdfSortOption.DATE_DESC) { onOptionSelected(PdfSortOption.DATE_DESC) }
        SortOptionRow("Última modificación (Antiguo)", Icons.Default.AccessTime, selectedOption == PdfSortOption.DATE_ASC) { onOptionSelected(PdfSortOption.DATE_ASC) }
        SortOptionRow("Nombre (A-Z)", Icons.Default.SortByAlpha, selectedOption == PdfSortOption.NAME_ASC) { onOptionSelected(PdfSortOption.NAME_ASC) }
        SortOptionRow("Nombre (Z-A)", Icons.Default.SortByAlpha, selectedOption == PdfSortOption.NAME_DESC) { onOptionSelected(PdfSortOption.NAME_DESC) }
        SortOptionRow("Tamaño (Mayor)", Icons.Default.DataUsage, selectedOption == PdfSortOption.SIZE_DESC) { onOptionSelected(PdfSortOption.SIZE_DESC) }
        SortOptionRow("Tamaño (Menor)", Icons.Default.DataUsage, selectedOption == PdfSortOption.SIZE_ASC) { onOptionSelected(PdfSortOption.SIZE_ASC) }
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F0F0)), modifier = Modifier.weight(1f).padding(end = 8.dp)) { Text("Cancelar", color = Color.Black) }
            Button(onClick = onApply, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), modifier = Modifier.weight(1f).padding(start = 8.dp)) { Text("Aceptar", color = Color.White) }
        }
    }
}

@Composable
fun SortOptionRow(text: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        CustomRadioButton(isSelected = isSelected)
    }
}

@Composable
fun CustomRadioButton(isSelected: Boolean) {
    Box(modifier = Modifier.size(24.dp).background(color = if (isSelected) Color(0xFFD32F2F) else Color.Transparent, shape = CircleShape).border(width = 2.dp, color = if (isSelected) Color(0xFFD32F2F) else Color.Gray, shape = CircleShape), contentAlignment = Alignment.Center) {
        if (isSelected) Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}