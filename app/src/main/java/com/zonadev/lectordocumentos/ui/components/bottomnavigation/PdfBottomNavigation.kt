package com.zonadev.lectordocumentos.ui.components.bottomnavigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zonadev.lectordocumentos.data.model.PdfTab

@Composable
fun PdfBottomNavigation(
    currentTab: PdfTab,
    onTabSelected: (PdfTab) -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.surface // Blanco en día, Gris oscuro en noche
    val contentColor = MaterialTheme.colorScheme.primary   // Rojo en día, Rojo pastel en noche

    // Color gris adaptable para lo no seleccionado
    val unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    NavigationBar(
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = 8.dp // Elevación sutil
    ) {
        // Iteramos sobre las entradas de la Sealed Class
        // Esto hace que agregar una pestaña nueva sea tan fácil como agregar una línea en PdfTab.kt
        PdfTab.entries.forEach { tab ->
            val isSelected = currentTab == tab

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = Color.White
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                },
                // Personalización de colores: Rojo al seleccionar, Gris al deseleccionar
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = contentColor,
                    selectedTextColor = contentColor,
                    indicatorColor = contentColor.copy(alpha = 0.12f), // Fondo suave rojo en el ícono seleccionado
                    unselectedIconColor = unselectedColor,
                    unselectedTextColor = unselectedColor
                )
            )
        }
    }
}