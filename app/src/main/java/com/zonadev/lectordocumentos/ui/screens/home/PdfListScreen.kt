package com.zonadev.lectordocumentos.ui.screens.home

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zonadev.lectordocumentos.core.PermissionHelper
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.ui.components.PdfRow
import com.zonadev.lectordocumentos.ui.components.PermissionCard

@Composable
fun PdfAppEntry(
    onOpenPdf: (Uri) -> Unit,
    viewModel: PdfListViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // --- 1. Launcher para permisos Legacy (Android 10 o inferior / Huawei) ---
    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.refresh()
        }
    }

    // --- 2. Manejo del Ciclo de Vida (Para volver de Configuración en Android 11+) ---
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Verificar permisos al volver a la app
                if (PermissionHelper.hasManageAllFiles(context)) {
                    // Solo refrescar si la UI cree que no tiene permiso o está vacía
                    if (state.needsPermission || state.pdfs.isEmpty()) {
                        viewModel.refresh()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // --- 3. Carga Inicial ---
    LaunchedEffect(Unit) {
        // Cargar solo si está vacío para evitar recargas dobles
        if (state.pdfs.isEmpty() && !state.isLoading) {
            viewModel.refresh()
        }
    }

    // --- 4. Renderizado ---
    PdfListScreen(
        isLoading = state.isLoading,
        pdfs = state.pdfs,
        needsPermission = state.needsPermission,
        isPermissionSkipped = state.isPermissionSkipped,
        onRefresh = { viewModel.refresh() },

        // --- LÓGICA DE PERMISOS HÍBRIDA ---
        onEnableManageAllFiles = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+: Abre Intent de "Acceso a todos los archivos"
                viewModel.requestManageAllFiles(context)
            } else {
                // Android 10- (Huawei, etc): Pide permiso clásico READ_EXTERNAL_STORAGE
                legacyPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        },

        onSkipPermission = { viewModel.skipPermissionRequest() },
        onOpenPdf = onOpenPdf
    )
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
    onOpenPdf: (Uri) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Mis Documentos PDF") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            when {
                // CASO A: Tarjeta Grande (Permiso requerido y NO saltado)
                needsPermission && !isPermissionSkipped -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        PermissionCard(
                            onManageAllFiles = onEnableManageAllFiles,
                            onSkip = onSkipPermission
                        )
                    }
                }

                // CASO B: Pantalla "Sin Permiso" (Usuario saltó la tarjeta)
                needsPermission && isPermissionSkipped -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Sin permiso",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Se requiere permiso para acceder a todos los archivos PDF.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onEnableManageAllFiles,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Permitir", color = Color.White)
                            }
                        }
                    }
                }

                // CASO C: Cargando
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                // CASO D: Lista Vacía
                pdfs.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No se encontraron archivos PDF")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = onRefresh) { Text("Refrescar") }
                        }
                    }
                }

                // CASO E: Lista Correcta
                else -> {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        // OPTIMIZACIÓN: 'key' ayuda a Compose a mantener la posición del scroll
                        items(items = pdfs, key = { it.id }) { pdf ->
                            PdfRow(pdf = pdf, onClick = { onOpenPdf(pdf.uri) })
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(onClick = onRefresh) { Text("Refrescar Lista") }
                    }
                }
            }
        }
    }
}