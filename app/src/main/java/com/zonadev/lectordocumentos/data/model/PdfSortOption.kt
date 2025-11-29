package com.zonadev.lectordocumentos.data.model

enum class PdfSortOption {
    DATE_DESC, // Última modificación (Reciente a antiguo) - Default
    DATE_ASC,  // Última modificación (Antiguo a reciente)
    NAME_ASC,  // Nombre (A-Z)
    NAME_DESC, // Nombre (Z-A)
    SIZE_DESC, // Tamaño (Mayor a menor)
    SIZE_ASC   // Tamaño (Menor a mayor)
}