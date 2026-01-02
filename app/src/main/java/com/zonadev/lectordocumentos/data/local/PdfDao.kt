package com.zonadev.lectordocumentos.data.local

import androidx.paging.PagingSource
import androidx.room.*
import com.zonadev.lectordocumentos.data.local.entities.LocalPdfEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {

    /* ─────────────────────────────
     * 🔹 CONSULTAS BÁSICAS
     * ───────────────────────────── */

    @Query("SELECT * FROM pdf_table WHERE id = :id")
    suspend fun getById(id: Long): LocalPdfEntity?

    /* ─────────────────────────────
     * ⭐ FAVORITOS (TAB GUARDADOS)
     * ───────────────────────────── */

    // Devuelve una fuente de paginación para la lista de favoritos.
    // 'COLLATE NOCASE' asegura que el orden alfabético ignore mayúsculas/minúsculas.
   /* @Query("""
        SELECT * FROM pdf_table
        WHERE isFavorite = 1
        ORDER BY name COLLATE NOCASE ASC
    """)
    fun getFavoritesPaged(): PagingSource<Int, LocalPdfEntity>*/

    @Query("SELECT * FROM pdf_table WHERE isFavorite = 1 ORDER BY name COLLATE NOCASE ASC")
    fun getFavoritesByNameAsc(): PagingSource<Int, LocalPdfEntity>

    @Query("SELECT * FROM pdf_table WHERE isFavorite = 1 ORDER BY name COLLATE NOCASE DESC")
    fun getFavoritesByNameDesc(): PagingSource<Int, LocalPdfEntity>

    @Query("SELECT * FROM pdf_table WHERE isFavorite = 1 ORDER BY lastModified DESC")
    fun getFavoritesByDateDesc(): PagingSource<Int, LocalPdfEntity>

    @Query("SELECT * FROM pdf_table WHERE isFavorite = 1 ORDER BY lastModified ASC")
    fun getFavoritesByDateAsc(): PagingSource<Int, LocalPdfEntity>

    @Query("SELECT * FROM pdf_table WHERE isFavorite = 1 ORDER BY size DESC")
    fun getFavoritesBySizeDesc(): PagingSource<Int, LocalPdfEntity>

    @Query("SELECT * FROM pdf_table WHERE isFavorite = 1 ORDER BY size ASC")
    fun getFavoritesBySizeAsc(): PagingSource<Int, LocalPdfEntity>

    // Actualización eficiente: Solo cambia el campo 'isFavorite' sin reescribir todo el objeto.
    @Query("UPDATE pdf_table SET isFavorite = :favorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, favorite: Boolean)

    /* ─────────────────────────────
     * 🕘 RECIENTES (TAB RECIENTES)
     * ───────────────────────────── */

    // Devuelve los archivos abiertos recientemente, ordenados por fecha de apertura (el más nuevo primero).
    @Query("""
        SELECT * FROM pdf_table
        WHERE lastOpenedTime IS NOT NULL
        ORDER BY lastOpenedTime DESC
    """)
    fun getRecentsPaged(): PagingSource<Int, LocalPdfEntity>

    // Actualiza la fecha de última apertura.
    @Query("UPDATE pdf_table SET lastOpenedTime = :timestamp WHERE id = :id")
    suspend fun updateLastOpened(id: Long, timestamp: Long)

    /* ─────────────────────────────
     * 💾 INSERCIÓN / ACTUALIZACIÓN
     * ───────────────────────────── */

    // Inserta un nuevo PDF o reemplaza uno existente si el ID coincide.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(pdf: LocalPdfEntity)

    /* ─────────────────────────────
     * 🗑 LIMPIEZA / ELIMINACIÓN
     * ───────────────────────────── */

    // Elimina un registro específico por su ID.
    @Query("DELETE FROM pdf_table WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT id FROM pdf_table WHERE isFavorite = 1")
    fun getAllFavoriteIdsFlow(): Flow<List<Long>>



    // Elimina todo (útil para desarrollo o limpiar caché).
    @Query("DELETE FROM pdf_table")
    suspend fun clearAll()
}