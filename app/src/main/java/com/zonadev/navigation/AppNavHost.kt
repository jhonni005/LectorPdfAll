package com.zonadev.lectordocumentos.navigation

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zonadev.lectordocumentos.ui.screens.home.PdfAppEntry
import com.zonadev.lectordocumentos.ui.screens.viewer.PdfViewerScreen

@Composable
fun AppNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = "list",
        // Opcional: Puedes definir la transición por defecto para todo el grafo aquí también
        // enterTransition = { EnterTransition.None },
        // exitTransition = { ExitTransition.None },
        // popEnterTransition = { EnterTransition.None },
        // popExitTransition = { ExitTransition.None }
    ) {
        // --- Pantalla 1: Lista de PDFs ---
        composable(
            route = "list",
            // Al salir hacia el visor: Corte directo (ninguna animación)
            exitTransition = { ExitTransition.None },
            // Al volver del visor: Aparece de golpe (ninguna animación)
            popEnterTransition = { EnterTransition.None }
        ) {
            PdfAppEntry(
                onOpenPdf = { uri ->
                    val encoded = Uri.encode(uri.toString())
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
            // Al entrar: Aparece de golpe
            enterTransition = { EnterTransition.None },
            // Al salir (Atrás): Desaparece de golpe
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