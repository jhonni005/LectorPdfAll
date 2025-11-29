package com.zonadev.lectordocumentos.ui.screens.home

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.data.model.PdfSortOption
import com.zonadev.lectordocumentos.ui.components.PdfRow
import com.zonadev.lectordocumentos.ui.components.PermissionCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfAppEntry(
    onOpenPdf: (Uri) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: PdfListViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val currentSortOption by viewModel.sortOption.collectAsState()
    // Usamos la lista ordenada
    val sortedPdfs by viewModel.sortedPdfs.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // skipPartiallyExpanded = true para evitar que el sheet se abra a medias
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    // Scroll al inicio al cambiar ordenamiento
    LaunchedEffect(currentSortOption) {
        listState.scrollToItem(0)
    }

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] == true ||
                permissions[android.Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
        if (isGranted) viewModel.refresh()
    }

    val onSortClick = { showBottomSheet = true }

    PdfListScreen(
        isLoading = state.isLoading,
        pdfs = sortedPdfs,
        needsPermission = state.needsPermission,
        isPermissionSkipped = state.isPermissionSkipped,
        onRefresh = { viewModel.refresh() },
        onEnableManageAllFiles = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                viewModel.requestManageAllFiles(context)
            } else {
                legacyPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        },
        onSkipPermission = { viewModel.skipPermissionRequest() },
        onOpenPdf = onOpenPdf,
        onSearchClick = onSearchClick,
        onSortClick = onSortClick,
        listState = listState
    )

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
                onCancel = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showBottomSheet = false
                    }
                },
                onApply = {
                    viewModel.updateSortOption(tempSelectedOption)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showBottomSheet = false
                    }
                }
            )
        }
    }
}

@Composable
fun SortBottomSheetContent(
    selectedOption: PdfSortOption,
    onOptionSelected: (PdfSortOption) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Ordenar por",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        SortOptionRow(
            text = "Última modificación (Reciente a antiguo)",
            icon = Icons.Default.History,
            isSelected = selectedOption == PdfSortOption.DATE_DESC,
            onClick = { onOptionSelected(PdfSortOption.DATE_DESC) }
        )
        SortOptionRow(
            text = "Última modificación (Antiguo a reciente)",
            icon = Icons.Default.AccessTime,
            isSelected = selectedOption == PdfSortOption.DATE_ASC,
            onClick = { onOptionSelected(PdfSortOption.DATE_ASC) }
        )
        SortOptionRow(
            text = "Nombre (A-Z)",
            icon = Icons.Default.SortByAlpha,
            isSelected = selectedOption == PdfSortOption.NAME_ASC,
            onClick = { onOptionSelected(PdfSortOption.NAME_ASC) }
        )
        SortOptionRow(
            text = "Nombre (Z-A)",
            icon = Icons.Default.SortByAlpha,
            isSelected = selectedOption == PdfSortOption.NAME_DESC,
            onClick = { onOptionSelected(PdfSortOption.NAME_DESC) }
        )
        SortOptionRow(
            text = "Tamaño del archivo (Más a menos)",
            icon = Icons.Default.DataUsage,
            isSelected = selectedOption == PdfSortOption.SIZE_DESC,
            onClick = { onOptionSelected(PdfSortOption.SIZE_DESC) }
        )
        SortOptionRow(
            text = "Tamaño del archivo (Menos a más)",
            icon = Icons.Default.DataUsage,
            isSelected = selectedOption == PdfSortOption.SIZE_ASC,
            onClick = { onOptionSelected(PdfSortOption.SIZE_ASC) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F0F0)),
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Text("Cancelar", color = Color.Black)
            }

            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            ) {
                Text("Aceptar", color = Color.White)
            }
        }
    }
}

@Composable
fun SortOptionRow(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = Color.Black
        )

        CustomRadioButton(isSelected = isSelected)
    }
}

@Composable
fun CustomRadioButton(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(
                color = if (isSelected) Color(0xFFD32F2F) else Color.Transparent,
                shape = CircleShape
            )
            .border(
                width = 2.dp,
                color = if (isSelected) Color(0xFFD32F2F) else Color.Gray,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfListScreen(
    isLoading: Boolean,
    pdfs: List<PdfItem>,
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
                    IconButton(onClick = onSortClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Ordenar")
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar PDF")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                needsPermission && !isPermissionSkipped -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        PermissionCard(onManageAllFiles = onEnableManageAllFiles, onSkip = onSkipPermission)
                    }
                }
                needsPermission && isPermissionSkipped -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sin permiso", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            Text("Se requiere permiso para leer los PDF.", textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = onEnableManageAllFiles, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                                Text("Permitir", color = Color.White)
                            }
                        }
                    }
                }
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                pdfs.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No se encontraron archivos PDF")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = onRefresh) { Text("Refrescar") }
                        }
                    }
                }
                else -> {
                    // --- LISTA OPTIMIZADA PARA RENDIMIENTO ---
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        state = listState
                    ) {
                        items(
                            items = pdfs,
                            key = { it.id }, // Clave única para evitar repintado
                            // contentType: Ayuda a Compose a reutilizar componentes de forma inteligente
                            contentType = { "pdf_row" }
                        ) { pdf ->
                            PdfRow(pdf = pdf, onClick = { onOpenPdf(pdf.uri) })
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
                        Button(onClick = onRefresh) { Text("Refrescar Lista") }
                    }
                }
            }
        }
    }
}