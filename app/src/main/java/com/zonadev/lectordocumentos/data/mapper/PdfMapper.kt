package com.zonadev.lectordocumentos.data.mapper


import android.net.Uri
import com.zonadev.lectordocumentos.data.local.entities.LocalPdfEntity
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.ui.utils.PdfFormatUtils

// ----------------------------------------------------------------------
// 1. De Base de Datos (Entity) -> A UI (PdfItem)
// ----------------------------------------------------------------------

fun LocalPdfEntity.toPdfItem(): PdfItem {
    // Usamos las utilidades centrales para formatear el texto igual que en MediaStore
    val sizeStr = PdfFormatUtils.formatSize(this.size)
    val dateStr = PdfFormatUtils.formatDate(this.lastModified)
    val detailsFinal = "$dateStr - $sizeStr"

    return PdfItem(
        id = this.id,
        name = this.name,
        uri = Uri.parse(this.uriString),
        // Nota: Room no suele guardar la ruta absoluta del sistema de archivos.
        // Si la necesitas para el diálogo de detalles, se puede obtener resolviendo la URI nuevamente,
        // pero para la lista, dejarla vacía es seguro y ahorra espacio en BD.
        path = "",
        lastModified = this.lastModified,
        size = this.size,
        details = detailsFinal,
        isFavorite = this.isFavorite // Mantenemos el estado real de la BD
    )
}

// ----------------------------------------------------------------------
// 2. De UI (PdfItem) -> A Base de Datos (Entity)
// ----------------------------------------------------------------------

/**
 * Convierte un PdfItem (generalmente venido de MediaStore) a una entidad de Room
 * para guardarlo como Favorito o Reciente.
 */
fun PdfItem.toLocalPdfEntity(isFavorite: Boolean = false, lastOpenedTime: Long? = null): LocalPdfEntity {
    return LocalPdfEntity(
        id = this.id,
        name = this.name,
        uriString = this.uri.toString(),
        size = this.size,
        lastModified = this.lastModified,
        isFavorite = isFavorite,
        lastOpenedTime = lastOpenedTime
    )
}