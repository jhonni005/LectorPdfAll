package com.zonadev.lectordocumentos.ui.screens.search

import android.app.Application
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.data.model.PdfSortOption
import com.zonadev.lectordocumentos.data.model.PdfTab
import com.zonadev.lectordocumentos.data.repository.PdfRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class PdfSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfRepository(application.applicationContext)

    // Estado del texto de búsqueda
    private val _searchText = MutableStateFlow(TextFieldValue(""))
    val searchText = _searchText.asStateFlow()

    // --- FLUJO DE RESULTADOS ---
    // Usamos flatMapLatest: cada vez que cambia el texto, cancela la búsqueda anterior
    // y lanza una nueva consulta paginada.
    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: Flow<PagingData<PdfItem>> =
        _searchText.flatMapLatest { tfv ->
        val query = tfv.text.trim()

        if (query.isBlank()) {
            // Si no hay texto, devolvemos un flujo vacío para no cargar nada
            flowOf(PagingData.empty())
        } else {
            // Si hay texto, buscamos.
            // Usamos un orden por defecto (ej. Fecha) para los resultados.
            repository.getPdfPager(
                tab = PdfTab.All,
                sortOption = PdfSortOption.DATE_DESC,
                query = query
            )
        }
    }.cachedIn(viewModelScope) // Mantiene el estado de la paginación mientras el VM viva

    fun onSearchTextChange(newValue: TextFieldValue) {
        _searchText.value = newValue
    }
}