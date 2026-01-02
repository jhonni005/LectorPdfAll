package com.zonadev.lectordocumentos.navigation

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zonadev.lectordocumentos.data.model.PdfTab
import com.zonadev.lectordocumentos.ui.screens.home.AllPdfsEntry
import com.zonadev.lectordocumentos.ui.screens.home.AllPdfsViewModel
import com.zonadev.lectordocumentos.ui.screens.home.PdfRecentScreen
import com.zonadev.lectordocumentos.ui.screens.home.PdfRecentViewModel
import com.zonadev.lectordocumentos.ui.screens.home.PdfSavedScreen
import com.zonadev.lectordocumentos.ui.screens.home.PdfSavedViewModel
import com.zonadev.lectordocumentos.ui.screens.search.PdfSearchScreen
import com.zonadev.lectordocumentos.ui.screens.tools.PdfTools
import com.zonadev.lectordocumentos.ui.screens.viewer.PdfViewerScreen


@Composable
fun AppNavHost(
    navController: NavHostController,
    allPdfsViewModel: AllPdfsViewModel,
    savedPdfsViewModel: PdfSavedViewModel
) {
    NavHost(
        navController = navController,
        startDestination = PdfTab.All.route,
    ) {
        // --- Pantalla 1: Lista de PDFs ---
        composable(
            route = PdfTab.All.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            AllPdfsEntry(
                viewModel = allPdfsViewModel,
                onOpenPdf = { uri ->
                    val encoded = Uri.encode(uri.toString())
                    navController.navigate("viewer?uri=$encoded") {
                        // restoreState = true
                    }
                },

                )
        }

        composable(
            PdfTab.Recent.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            val recentViewModel: PdfRecentViewModel = viewModel()
            PdfRecentScreen(
                onOpenPdf = { uri ->
                    val encoded = Uri.encode(uri.toString())
                    navController.navigate("viewer?uri=$encoded") {
                        // restoreState = true
                    }
                },
                onSearchClick = {},
                viewModel = recentViewModel
            )
        }

        composable(
            PdfTab.Saved.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            PdfSavedScreen(
                onOpenPdf = { uri ->
                    val encoded = Uri.encode(uri.toString())
                    navController.navigate("viewer?uri=$encoded") {
                        // restoreState = true
                    }
                },
                viewModel = savedPdfsViewModel
            )
        }

        composable(
            PdfTab.Tools.route, enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }) {
            PdfTools()
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
                onBack = {
                    navController.popBackStack()
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