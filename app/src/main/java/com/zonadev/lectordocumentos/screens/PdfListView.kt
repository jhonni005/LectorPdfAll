package com.zonadev.lectordocumentos.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zonadev.lectordocumentos.model.PdfItem
import com.zonadev.lectordocumentos.utils.PermissionHelper
import kotlinx.coroutines.delay



@Composable
fun PdfAppEntry(appContext: Context) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val presenter = remember { PdfListPresenter(appContext) }

    val (isLoading, setLoading) = remember { mutableStateOf(false) }
    val (pdfs, setPdfs) = remember { mutableStateOf<List<PdfItem>>(emptyList()) }
    val (needsPermission, setNeedsPermission) = remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val viewImpl = object : PdfListPresenter.View {
            override fun showLoading() {
                setLoading(true)
                setNeedsPermission(false)
            }

            override fun showPdfs(pdfsList: List<PdfItem>) {
                setLoading(false)
                setPdfs(pdfsList)
                setNeedsPermission(false)
            }

            override fun showPermissionRequired() {
                setLoading(false)
                setNeedsPermission(true)
                setPdfs(emptyList())
            }
        }
        presenter.attach(viewImpl)
        onDispose { presenter.detach() }
    }

    // ✅ Verifica permiso solo al iniciar
    //no se ejecuta en huawei
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (PermissionHelper.hasManageAllFiles(context)) {
            presenter.refresh()
        } else {
         setNeedsPermission(true)
            setPdfs(emptyList())
        }
    }

    PdfListScreen(
        isLoading = isLoading,
        pdfs = pdfs,
        needsPermission = needsPermission,
        onRefresh = { presenter.refresh() },
        onEnableManageAllFiles = {
            presenter.requestManageAllFiles()
        },
        onOpenPdf = { uri ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                appContext.startActivity(intent)
            } catch (_: Exception) {}
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfListScreen(
    isLoading: Boolean,
    pdfs: List<PdfItem>,
    needsPermission: Boolean,
    onRefresh: () -> Unit,
    onEnableManageAllFiles: () -> Unit,
    onOpenPdf: (Uri) -> Unit
) {
    val context = LocalContext.current

    // ✅ Lógica de verificación continua del permiso
    LaunchedEffect(needsPermission) {
        if (PermissionHelper.hasManageAllFiles(context)) {
            onRefresh()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("PDFs") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (needsPermission) {
                PermissionCard(onManageAllFiles = onEnableManageAllFiles)
            } else if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (pdfs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No se encontraron PDFs")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(pdfs) { pdf ->
                            PdfRow(pdf = pdf, onClick = { onOpenPdf(pdf.uri) })
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Button(onClick = onRefresh) { Text("Refrescar") }
            }
        }
    }
}

@Composable
fun PermissionCard(onManageAllFiles: () -> Unit) {
    val context = LocalContext.current
    val permissionToRequest = Manifest.permission.READ_EXTERNAL_STORAGE

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, permissionToRequest
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (isGranted) {
                onManageAllFiles()
            }
        }
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Esta app necesita permiso para listar PDFs",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        onManageAllFiles()
                    } else {
                        launcher.launch(permissionToRequest)
                    }
                }
            ) {
                Text("Habilitar acceso a todos los archivos")
            }

            if (hasPermission) {
                Text(
                    "✅ Permiso concedido",
                    color = Color.Green,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun PdfRow(pdf: PdfItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(text = pdf.name, style = MaterialTheme.typography.bodyLarge)
    }
}