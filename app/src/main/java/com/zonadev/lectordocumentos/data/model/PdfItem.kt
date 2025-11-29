package com.zonadev.lectordocumentos.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable


@Immutable
data class PdfItem(
    val id: Long,
    val name: String,
    val uri: Uri,
    val lastModified: Long, // Fecha en milisegundos
    val size: Long,
    val displayDate: String,
    val displaySize: String// Tamaño en bytes
)