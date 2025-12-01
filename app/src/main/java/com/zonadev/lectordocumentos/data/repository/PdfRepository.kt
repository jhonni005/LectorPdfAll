package com.zonadev.lectordocumentos.data.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.zonadev.lectordocumentos.data.datasource.PdfPagingSource
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.data.model.PdfSortOption
import kotlinx.coroutines.flow.Flow

class PdfRepository(private val context: Context) {

    // Esta función crea el flujo de datos paginados.
    // Paging 3 se encarga de llamar a tu 'PdfPagingSource' automáticamente
    // para pedir más datos cuando el usuario hace scroll.
    fun getPdfPager(sortOption: PdfSortOption, query: String): Flow<PagingData<PdfItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 30,        // Carga bloques de 20 en 20 (Muy rápido y ligero en RAM)
                enablePlaceholders = false, // No mostramos "huecos" vacíos
                prefetchDistance = 5, // Empieza a cargar la siguiente página cuando falten 10 items
                initialLoadSize = 30,   // La primera carga también es pequeña para que la app abra al instante
                maxSize = 200

            ),
            pagingSourceFactory = {
                // Cada vez que la lista se invalida (nuevo filtro/orden),
                // se crea una nueva fuente de datos limpia.
                PdfPagingSource(context, sortOption, query)
            }
        ).flow
    }
}