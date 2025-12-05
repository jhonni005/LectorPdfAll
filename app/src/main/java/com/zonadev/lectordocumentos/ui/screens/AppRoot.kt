package com.zonadev.lectordocumentos.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zonadev.lectordocumentos.navigation.AppNavHost
import com.zonadev.lectordocumentos.ui.components.bottomnavigation.PdfBottomNavigation
import com.zonadev.lectordocumentos.ui.screens.home.PdfListViewModel

@Composable
fun AppRoot() {
    val navController = rememberNavController()

    // 1. Creamos el ViewModel AQUÍ (Nivel Superior)
    // Esto permite que el BottomBar y la Lista compartan el mismo estado (qué pestaña está seleccionada)
    val sharedViewModel: PdfListViewModel = viewModel()

    val currentTab by sharedViewModel.currentTab.collectAsState()

    val uiState by sharedViewModel.uiState.collectAsState()

    // 2. Detectamos en qué pantalla estamos
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Solo mostramos la barra inferior en la pantalla principal ("list")
    val showBottomBar = currentRoute == "list" && !uiState.needsPermission

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                PdfBottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        sharedViewModel.onTabSelected(tab)
                    }
                )
            }
        }
    ) { innerPadding ->
        // 3. Pasamos el padding y el ViewModel compartido a la navegación
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AppNavHost(
                navController = navController,
                sharedViewModel = sharedViewModel
            )
        }
    }
}