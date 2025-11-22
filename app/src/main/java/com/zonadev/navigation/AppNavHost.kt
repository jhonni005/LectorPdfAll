package com.zonadev.navigation

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zonadev.lectordocumentos.screens.PdfAppEntry
import com.zonadev.lectordocumentos.ui.PdfiumViewerScreen
@Composable
fun AppNavHost(
    navController: NavHostController,
    appContext: Context
) {
    NavHost(
        navController = navController,
        startDestination = "list"
    ) {

        composable("list") {
            PdfAppEntry(
                appContext = appContext,
                onOpenPdf = { uri ->
                    val encoded = Uri.encode(uri.toString())
                    navController.navigate("viewer?uri=$encoded")
                }
            )
        }

        composable(
            route = "viewer?uri={uri}",
            arguments = listOf(
                navArgument("uri") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val uriArg = backStackEntry.arguments?.getString("uri")
            if (uriArg == null) return@composable

            val pdfUri = Uri.parse(uriArg)

            PdfiumViewerScreen(
                pdfUri = pdfUri,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
