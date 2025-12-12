package com.zonadev.lectordocumentos.ui.screens.home

import android.Manifest
import android.net.Uri
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
import androidx.paging.compose.LazyPagingItems
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.data.model.PdfTab
import com.zonadev.lectordocumentos.ui.utils.StableTopPadding



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








