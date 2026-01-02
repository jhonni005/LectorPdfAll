package com.zonadev.lectordocumentos.ui.screens.home


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.zonadev.lectordocumentos.core.PdfActions
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.data.model.PdfSortOption
import com.zonadev.lectordocumentos.data.model.PdfTab
import com.zonadev.lectordocumentos.data.repository.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class PdfSavedViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfRepository(application.applicationContext)
    private val pdfActions = PdfActions(repository, application)

    // --- ESTADOS DE SCROLL ---
    var lastScrollIndex: Int? = null
    var lastScrollOffset: Int? = null
    var isRestoringScroll = false

    // --- ESTADOS DE UI ---
    private val _sortOption = MutableStateFlow(PdfSortOption.DATE_DESC)
    val sortOption = _sortOption.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    private val _selectedPdfForOptions = MutableStateFlow<PdfItem?>(null)
    val selectedPdfForOptions = _selectedPdfForOptions.asStateFlow()

    private val _isPdfFavorite = MutableStateFlow(false)
    val isPdfFavorite = _isPdfFavorite.asStateFlow()

    val favoriteIds: StateFlow<Set<Long>> = repository.getFavoriteIdsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly, // Mantiene el estado caliente y listo para la UI
            initialValue = emptySet()
        )



    // --- FLUJO DE DATOS (ROOM) ORIGINAL ---
   /* @OptIn(ExperimentalCoroutinesApi::class)
    val pagedSavedPdfs: Flow<PagingData<PdfItem>> =
        _sortOption
        .flatMapLatest { sort ->
            sort
            // Pedimos al repositorio el Pager configurado para la pestaña 'Saved'
            repository.getPdfPager(
                tab = PdfTab.Saved,
                sortOption = sort,
                query = ""
            )
        }
        .cachedIn(viewModelScope)*/

    //PROBANDO
    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedSavedPdfs: Flow<PagingData<PdfItem>> = combine(
        _sortOption,
        _refreshTrigger
    ) { sort, _ ->
        sort
    }.flatMapLatest { sort ->
        // Aquí es donde el Repository usa tus 6 consultas del DAO según el 'sort'
        repository.getPdfPager(
            tab = PdfTab.Saved,
            sortOption = sort,
            query = "" // Sin búsqueda, como solicitaste
        )
    }.cachedIn(viewModelScope)



    // --- ACCIONES ORQUESTADAS POR PDFACTIONS ---

    fun showPdfOptions(pdf: PdfItem) {
        _selectedPdfForOptions.value = pdf
        // Obtenemos el valor más reciente de favoritos de la BD para mostrar el icono correcto
        viewModelScope.launch(Dispatchers.IO) {
            val isFav = repository.getFavoriteStatus(pdf.id)
            _isPdfFavorite.value = isFav
        }
    }

    fun hidePdfOptions() {
        _selectedPdfForOptions.value = null
    }

    /**
     * Alterna el estado de favorito.
     * Nota: En esta pestaña, si isFavorite pasa a false, el item desaparecerá de la lista.
     */
    fun toggleFavorite(pdf: PdfItem, isFavorite: Boolean) {
        viewModelScope.launch {
            pdfActions.toggleFavorite(pdf, isFavorite)
            _isPdfFavorite.value = isFavorite
        }
    }

    /**
     * Elimina el archivo físico y el registro en Room.
     */
    fun deletePdf(pdf: PdfItem) {
        viewModelScope.launch {
            pdfActions.delete(pdf) {
                _refreshTrigger.value++
            }
        }
    }

    /**
     * Renombra el archivo y actualiza los metadatos.
     */
    fun renamePdf(pdf: PdfItem, newName: String) {
        viewModelScope.launch {
            pdfActions.rename(pdf, newName) {
                _refreshTrigger.value++
            }
        }
    }

    fun updateSortOption(newOption: PdfSortOption) {
        if (_sortOption.value != newOption) {
            _sortOption.value = newOption
        }
    }


    /**
     * Registra la apertura del documento para la pestaña de Recientes.
     */
    fun markAsOpened(pdf: PdfItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markPdfAsOpened(pdf)
        }
    }
}