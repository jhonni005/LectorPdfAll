package com.zonadev.lectordocumentos.navigation

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zonadev.lectordocumentos.ui.screens.home.PdfAppEntry
import com.zonadev.lectordocumentos.ui.screens.home.PdfListViewModel
import com.zonadev.lectordocumentos.ui.screens.home.PdfSearchScreen
import com.zonadev.lectordocumentos.ui.screens.viewer.PdfViewerScreen


@Composable
fun AppNavHost(
    navController: NavHostController,
    sharedViewModel: PdfListViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "list",
    ) {
        // --- Pantalla 1: Lista de PDFs ---
        composable(
            route = "list",
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None }
        ) {
            PdfAppEntry(
                viewModel = sharedViewModel,
                onOpenPdf = { uri ->
                    val encoded = Uri.encode(uri.toString())
                    navController.navigate("viewer?uri=$encoded")
                },
                onSearchClick = {
                    //navegamos a la pantalal de busqueda
                    navController.navigate("search")
                }
            )
        }
        // --- 2. PANTALLA DE BÚSQUEDA (Nueva) ---
        composable(
            route = "search",
            // Sin animaciones de entrada/salida
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            PdfSearchScreen(
                viewModel = sharedViewModel,
                onBack = {
                    navController.popBackStack()
                    sharedViewModel.onSearchTextChange(TextFieldValue(""))
                         },
                onOpenPdf = { uri ->
                    val encoded = Uri.encode(uri.toString())
                    // Desde la búsqueda también podemos ir directo al visor
                    navController.navigate("viewer?uri=$encoded")
                }
            )
        }


        // --- Pantalla 2: Visor de PDF ---
        composable(
            route = "viewer?uri={uri}",
            arguments = listOf(
                navArgument("uri") { type = NavType.StringType }
            ),
            enterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) { backStackEntry ->
            val uriArg = backStackEntry.arguments?.getString("uri")

            if (uriArg != null) {
                val pdfUri = uriArg.toUri()

                PdfViewerScreen(
                    pdfUri = pdfUri,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}