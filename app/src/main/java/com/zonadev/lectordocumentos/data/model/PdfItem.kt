package com.zonadev.lectordocumentos.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class PdfItem(
    val id: Long,
    val name: String,
    val uri: Uri,
    val lastModified: Long,
    val size: Long,
    // Este campo ya viene listo del repositorio ("12 mar. 2023 - 4 MB")
    // para que la UI no pierda tiempo calculando.
    val details: String
)