package com.zonadev.lectordocumentos.ui.screens.home

import android.app.AppOpsManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.zonadev.lectordocumentos.core.PermissionHelper
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.data.model.PdfSortOption
import com.zonadev.lectordocumentos.data.model.PdfTab
import com.zonadev.lectordocumentos.data.repository.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class PdfListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfRepository(application.applicationContext)
    private var appOpsListener: AppOpsManager.OnOpChangedListener? = null

    // Cadena manual para evitar error de referencia en algunas versiones de compilador
    private val OP_MANAGE_EXTERNAL_STORAGE = "android:manage_external_storage"

    // Estado UI (Solo Permisos y carga inicial de permisos)
    // Nota: La lista de PDFs ya no está aquí, vive en el Flow 'pagedPdfList'
    data class PdfListUiState(
        val needsPermission: Boolean = false,
        val isPermissionSkipped: Boolean = false
    )

    private val _uiState = MutableStateFlow(PdfListUiState())
    val uiState: StateFlow<PdfListUiState> = _uiState.asStateFlow()

    // --- FILTROS SEARCH---
    private val _searchText = MutableStateFlow(TextFieldValue(""))
    val searchText = _searchText.asStateFlow()

    private val _sortOption = MutableStateFlow(PdfSortOption.DATE_DESC)
    val sortOption = _sortOption.asStateFlow()


    // 3 puntitos - Opciones ---
    private val _selectedPdfForOptions = MutableStateFlow<PdfItem?>(null)
    val selectedPdfForOptions = _selectedPdfForOptions.asStateFlow()


    //Estado pestaña actual : BottomNavigation
    // 1. Nuevo Estado: Pestaña Actual
    private val _currentTab = MutableStateFlow<PdfTab>(PdfTab.All)
    val currentTab = _currentTab.asStateFlow()

    // Función para abrir el menú
    fun showPdfOptions(pdf: PdfItem) {
        _selectedPdfForOptions.value = pdf
    }

    // Función para cerrar el menú
    fun hidePdfOptions() {
        _selectedPdfForOptions.value = null
    }

    // --- FLUJO PAGINADO MAESTRO ---
    // Esta es la clave del rendimiento. Combina (Búsqueda + Orden).
    // Si escribes una letra o cambias el orden, 'flatMapLatest' cancela la carga anterior
    // y solicita una nueva paginación SQL optimizada.
    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedPdfList: Flow<PagingData<PdfItem>> = combine(
        _searchText,
        _sortOption,
        _currentTab
    ) { tfv, sort , tab ->
        Triple(tfv.text, sort, tab)
    }.flatMapLatest { (query, sort, tab) ->
        // Llamamos al repositorio que crea el PagingSource con la query SQL exacta
        repository.getPdfPager(sort, query)
    }
        .cachedIn(viewModelScope) // Cachear en ViewModel para sobrevivir rotaciones de pantalla

    fun onSearchTextChange(newValue: TextFieldValue) {
        _searchText.value = newValue
    }

    fun updateSortOption(newOption: PdfSortOption) {
        _sortOption.value = newOption
    }

    // Acción para cambiar pestaña
    fun onTabSelected(tab: PdfTab) {
        _currentTab.value = tab
    }

    // --- INICIALIZACIÓN ---
    init {
        checkPermissions()
    }

    // Llamado al iniciar o al volver de settings
    fun refresh() {
        checkPermissions()
        // Nota: Con Paging 3, el refresco de datos se hace en la UI llamando a .refresh()
        // sobre el objeto LazyPagingItems, no aquí.
    }

    private fun checkPermissions() {
        val context = getApplication<Application>().applicationContext
        if (PermissionHelper.hasManageAllFiles(context)) {
            stopWatchingPermission()
            _uiState.value = _uiState.value.copy(needsPermission = false)
        } else {
            _uiState.value = _uiState.value.copy(needsPermission = true)
        }
    }

    fun skipPermissionRequest() {
        _uiState.value = _uiState.value.copy(isPermissionSkipped = true)
        stopWatchingPermission()
    }

    // --- GESTIÓN DE PERMISOS (Lógica Nativa) ---
    fun requestManageAllFiles(activityContext: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // 1. Iniciamos vigilancia del sistema
                startWatchingPermission()

                // 2. Abrimos Settings SIN flag NEW_TASK para mantener la pila unida
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
                        // A. Traer App al frente (cierra visualmente Settings)
                        bringAppToFront(context)

                        // B. Verificar estado
                        viewModelScope.launch(Dispatchers.Main) {
                            checkPermissions()
                        }
                    }
                }
            }
            try {
                appOps.startWatchingMode(
                    OP_MANAGE_EXTERNAL_STORAGE,
                    context.packageName,
                    appOpsListener!!
                )
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
        try {
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.let {
                it.flags = 0
                // CLEAR_TOP destruye la actividad de Settings que está encima
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP)
                context.startActivity(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopWatchingPermission()
    }
}