package com.zonadev.lectordocumentos.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zonadev.lectordocumentos.data.model.PdfTab
import com.zonadev.lectordocumentos.navigation.AppNavHost
import com.zonadev.lectordocumentos.ui.components.SortBottomSheetContent
import com.zonadev.lectordocumentos.ui.components.bottomnavigation.PdfBottomNavigation
import com.zonadev.lectordocumentos.ui.screens.home.AllPdfsViewModel
import com.zonadev.lectordocumentos.ui.screens.home.PdfSavedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val navController = rememberNavController()

    // ViewModel para permisos globales (sigue siendo útil aquí)
    val allPdfsViewModel: AllPdfsViewModel = viewModel()
    val savedPdfsViewModel: PdfSavedViewModel = viewModel()
    val uiState by allPdfsViewModel.uiState.collectAsState()

    // Obtenemos la ruta actual desde el NavController
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Definimos qué rutas deben mostrar la barra inferior
    // Verificamos si la ruta actual pertenece a alguna de las pestañas principales
    val currentTab = PdfTab.entries.find { it.route == currentRoute }
    val isMainTab = PdfTab.entries.any { it.route == currentRoute }
    val showBottomBar = isMainTab && !uiState.needsPermission
    var showSortSheet by remember { mutableStateOf(false) }

    val topBarTitle = when (currentTab) {
        PdfTab.All -> "Todos los PDF"
        PdfTab.Recent -> "Recientes"
        PdfTab.Saved -> "Guardados"
        PdfTab.Tools -> "Herramientas"
        else -> ""
    }

    Scaffold(
        topBar = {
            if (showBottomBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = topBarTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    actions = {
                        if (currentTab == PdfTab.All || currentTab == PdfTab.Saved){
                            IconButton(onClick = {showSortSheet = true}) { Icon(Icons.AutoMirrored.Filled.Sort, "Ordenar") }
                        }
                        IconButton(onClick = {navController.navigate("search")}) { Icon(Icons.Default.Search, "Buscar") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )

            }
        },
        bottomBar = {
            if (showBottomBar) {
                PdfBottomNavigation(
                    currentRoute = currentRoute,
                    onTabSelected = { tab ->
                        // Navegación optimizada para Bottom Bars
                        navController.navigate(tab.route) {
                            // Vuelve a la pantalla inicial del grafo para evitar acumular pilas
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Evita múltiples copias de la misma pantalla
                            launchSingleTop = true
                            // Restaura el estado (scroll, etc) al volver a la pestaña
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
              //  .padding(bottom = innerPadding.calculateBottomPadding())

        ) {
            AppNavHost(
                navController = navController,
                allPdfsViewModel = allPdfsViewModel,
                savedPdfsViewModel = savedPdfsViewModel
            )
        }
    }

    val allSortOption by allPdfsViewModel.sortOption.collectAsState()
    val savedSortOption by savedPdfsViewModel.sortOption.collectAsState()

    val currentSortOption = if (currentTab == PdfTab.Saved) savedSortOption else allSortOption

    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ) {
            var tempSelectedOption by remember {
                mutableStateOf(currentSortOption)
            }

            SortBottomSheetContent(
                selectedOption = tempSelectedOption,
                onOptionSelected = { tempSelectedOption = it },
                onCancel = {
                    showSortSheet = false
                },
                onApply = {
                    when (currentTab) {
                        PdfTab.All -> allPdfsViewModel.updateSortOption(tempSelectedOption)
                        PdfTab.Saved -> savedPdfsViewModel.updateSortOption(tempSelectedOption)
                        else -> Unit
                    }
                    showSortSheet = false
                }
            )
        }
    }
}