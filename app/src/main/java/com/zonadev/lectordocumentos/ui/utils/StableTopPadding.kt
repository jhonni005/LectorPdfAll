package com.zonadev.lectordocumentos.ui.utils

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun StableTopPadding(): Dp {

    val density = LocalDensity.current
    val insets = WindowInsets.statusBars

    // Altura actual en tiempo real
    val currentHeightPx = insets.getTop(density)
    val currentHeightDp = with(density) { currentHeightPx.toDp() }

    // Memoria: Guardamos la última altura válida mayor a 0
    var stableHeight by remember { mutableStateOf(0.dp) }

    if (currentHeightDp > 0.dp) {
        stableHeight = currentHeightDp
    }

    // Lógica de estabilidad:
    // Si la altura actual es 0 (se ocultó), usamos la guardada.
    // Si la altura actual es > 0 (normal), usamos la actual.
    return if (currentHeightDp > 0.dp) currentHeightDp else stableHeight
}
