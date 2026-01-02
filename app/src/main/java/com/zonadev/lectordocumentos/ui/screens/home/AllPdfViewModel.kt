package com.zonadev.lectordocumentos.ui.screens.home

import android.app.AppOpsManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.zonadev.lectordocumentos.core.PdfActions
import com.zonadev.lectordocumentos.core.PermissionHelper
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.data.model.PdfSortOption
import com.zonadev.lectordocumentos.data.model.PdfTab
import com.zonadev.lectordocumentos.data.repository.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * AllPdfsViewModel: Gestiona la lógica de la pestaña principal "Todos".
 */

class AllPdfsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfRepository(application.applicationContext)
    private val pdfActions = PdfActions(repository, application)

    private val OP_MANAGE_EXTERNAL_STORAGE = "android:manage_external_storage"
    private var appOpsListener: AppOpsManager.OnOpChangedListener? = null

    // --- ESTADOS DE SCROLL ---
    var lastScrollIndex: Int? = null
    var lastScrollOffset: Int? = null
    var isRestoringScroll = false



    // --- ESTADOS DE UI ---
    data class AllPdfsUiState(
        val needsPermission: Boolean = false,
        val isPermissionSkipped: Boolean = false
    )

    private val _uiState = MutableStateFlow(AllPdfsUiState())
    val uiState = _uiState.asStateFlow()

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

    /*
       2. LISTA PAGINADA PURA
       Solo se encarga de traer datos y ordenarlos. No se recombina cada vez que das like.
       Esto evita el error "Attempt to collect twice" y mejora el rendimiento del scroll.
    */
    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedPdfList: Flow<PagingData<PdfItem>> = combine(
        _sortOption,
        _refreshTrigger
    ) { sort, _ -> sort }
        .flatMapLatest { sort ->
            repository.getPdfPager(PdfTab.All, sort, query = "")
        }
        .cachedIn(viewModelScope) // Único cache necesario



    // --- GESTIÓN DE PERMISOS ---
    init {
        checkPermissions()
    }

    fun refresh() {
        checkPermissions()
        _refreshTrigger.value++
    }

    private fun checkPermissions() {
        val context = getApplication<Application>().applicationContext
        val hasPermission = PermissionHelper.hasManageAllFiles(context)
        if (hasPermission) stopWatchingPermission()
        _uiState.value = _uiState.value.copy(needsPermission = !hasPermission)
    }

    fun skipPermissionRequest() {
        _uiState.value = _uiState.value.copy(isPermissionSkipped = true)
        stopWatchingPermission()
    }

    fun requestManageAllFiles(activityContext: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startWatchingPermission()
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${activityContext.packageName}")
                }
                activityContext.startActivity(intent)
            } else {
                val intent = PermissionHelper.manageAllFilesIntentSafe(activityContext)
                    ?: PermissionHelper.appSettingsIntent(activityContext)
                activityContext.startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(activityContext, "Error al abrir configuración", Toast.LENGTH_LONG).show()
        }
    }

    private fun startWatchingPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val context = getApplication<Application>().applicationContext
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return
            appOpsListener = AppOpsManager.OnOpChangedListener { op, packageName ->
                if (packageName == context.packageName && OP_MANAGE_EXTERNAL_STORAGE == op) {
                    if (PermissionHelper.hasManageAllFiles(context)) {
                        bringAppToFront(context)
                        viewModelScope.launch(Dispatchers.Main) { checkPermissions() }
                    }
                }
            }
            try {
                appOps.startWatchingMode(OP_MANAGE_EXTERNAL_STORAGE, context.packageName, appOpsListener!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopWatchingPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && appOpsListener != null) {
            val context = getApplication<Application>().applicationContext
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            try {
                appOps?.stopWatchingMode(appOpsListener!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            appOpsListener = null
        }
    }

    private fun bringAppToFront(context: Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.let {
            it.flags = 0
            it.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            context.startActivity(it)
        }
    }

    // --- ACCIONES ORQUESTADAS ---
    fun updateSortOption(newOption: PdfSortOption) {
        _sortOption.value = newOption
    }

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

    fun toggleFavorite(pdf: PdfItem, isFavorite: Boolean) {
        viewModelScope.launch {
            pdfActions.toggleFavorite(pdf, isFavorite)
            _isPdfFavorite.value = isFavorite
        }
    }

    fun deletePdf(pdf: PdfItem) {
        viewModelScope.launch {
            pdfActions.delete(pdf) {
                // El PagingSource de MediaStore se refrescará con el trigger si es necesario,
                // pero Room lo hará automáticamente por reactividad.
                _refreshTrigger.value++
            }
        }
    }

    fun renamePdf(pdf: PdfItem, newName: String) {
        viewModelScope.launch {
            pdfActions.rename(pdf, newName) {
                _refreshTrigger.value++
            }
        }
    }

    fun markPdfAsOpened(pdf: PdfItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markPdfAsOpened(pdf)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopWatchingPermission()
    }
}