package com.zonadev.lectordocumentos.data.local.entities


import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa un PDF en la base de datos local (Room).
 * Se utiliza para persistir estados como 'favorito' y 'última vez abierto'
 * que no existen en el MediaStore de Android.
 */
@Entity(tableName = "pdf_table")
data class LocalPdfEntity(
    // El ID debe coincidir con MediaStore.Files.FileColumns._ID para mantener la relación
    @PrimaryKey val id: Long,

    val name: String,
    val uriString: String, // Room no guarda Uri directamente, usamos String
    val size: Long,
    val lastModified: Long,

    // Campos exclusivos de nuestra App
    val isFavorite: Boolean = false,
    val lastOpenedTime: Long? = null // Timestamp en milisegundos, null si nunca se abrió
)