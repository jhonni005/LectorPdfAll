package com.zonadev.lectordocumentos.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

sealed class PdfTab(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    // Definición de las pestañas
    data object All : PdfTab("all", "Todos", Icons.Default.Home)
    data object Recent : PdfTab("recent", "Reciente", Icons.Default.AccessTime)
    data object Saved : PdfTab("saved", "Guardados", Icons.Default.Bookmark)
    data object Tools : PdfTab("tools", "Herramientas", Icons.Default.Build)

    companion object {
        // 🔥 CORRECCIÓN CRÍTICA: Usamos 'get()' (getter personalizado)
        // Esto asegura que la lista se cree SOLO cuando se pide, evitando que
        // se lea como 'null' durante el arranque de la app.
        val entries get() = listOf(All, Recent, Saved, Tools)
    }
}