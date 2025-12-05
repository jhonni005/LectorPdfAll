package com.zonadev.lectordocumentos.ui.theme

import androidx.compose.ui.graphics.Color

// --- MODO CLARO (Tus colores actuales) ---
val RedPrimaryLight = Color(0xFFD32F2F) // Rojo intenso marca
val BackgroundLight = Color(0xFFFDFDFD) // Blanco casi puro
val SurfaceLight = Color(0xFFFFFFFF)    // Blanco puro para tarjetas/barras
val OnSurfaceLight = Color(0xFF1C1B1F)  // Texto casi negro

// --- MODO OSCURO (Profesional Google) ---
// Google recomienda desaturar el color primario en modo oscuro
val RedPrimaryDark = Color(0xFFFF5449)  // Rojo pastel/salmón (mejor contraste en oscuro)

// "Gris Suave" recomendado por Material Design 3 (no es negro #000000)
val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)     // Un poco más claro para la barra de navegación y tarjetas
val OnSurfaceDark = Color(0xFFE6E1E5)   // Texto blanco suave (no quema la vista)

// Colores de error (estándar)
val ErrorLight = Color(0xFFBA1A1A)
val ErrorDark = Color(0xFFFFB4AB)