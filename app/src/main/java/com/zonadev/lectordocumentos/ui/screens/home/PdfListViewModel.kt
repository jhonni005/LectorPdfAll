package com.zonadev.lectordocumentos.ui.screens.home

import android.app.AppOpsManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map as pagingDataMap // 🔥 ALIAS: Evita colisión con Flow.map
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PdfListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfRepository(application.applicationContext)
    private var appOpsListener: AppOpsManager.OnOpChangedListener? = null

    // Cadena manual para evitar error de referencia en algunas versiones de compilador
    private val OP_MANAGE_EXTERNAL_STORAGE = "android:manage_external_storage"

    var lastScrollIndex: Int? = null
    var lastScrollOffset: Int? = null
    var isRestoringScroll = false

    private val _isPdfFavorite = MutableStateFlow(false)
    val isPdfFavorite: StateFlow<Boolean> = _isPdfFavorite.asStateFlow()


    // Estado UI (Solo Permisos y carga inicial de permisos)
    // Nota: La lista de PDFs ya no está aquí, vive en el Flow 'pagedPdfList'
    data class PdfListUiState(
        val needsPermission: Boolean = false,
        val isPermissionSkipped: Boolean = false
    )

    private val _uiState = MutableStateFlow(PdfListUiState())
    val uiState: StateFlow<PdfListUiState> = _uiState.asStateFlow()

    private val _sortOption = MutableStateFlow(PdfSortOption.DATE_DESC)
    val sortOption = _sortOption.asStateFlow()

    // Trigger para forzar recarga (ej. al renombrar)
    private val _refreshTrigger = MutableStateFlow(0)

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
        viewModelScope.launch(Dispatchers.IO) {
            _isPdfFavorite.value = repository.getFavoriteStatus(pdf.id)
        }
    }

    fun hidePdfOptions() {
        _selectedPdfForOptions.value = null
    }
    // --- FLUJO PAGINADO MAESTRO ---
    // Esta es la clave del rendimiento. Combina (Búsqueda + Orden).
    // Si escribes una letra o cambias el orden, 'flatMapLatest' cancela la carga anterior
    // y solicita una nueva paginación SQL optimizada.
    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedPdfList: Flow<PagingData<PdfItem>> = combine(
        _sortOption,
        _currentTab,
        _refreshTrigger,
        repository.getFavoriteIdsFlow()
    ) { sort, tab, _, favoriteIds ->
        DataRequest(sort, tab, favoriteIds)
    }.flatMapLatest { request ->
        // 1. Obtenemos el flujo de PagingData
        repository.getPdfPager(request.tab, request.sort, query = "")
            .map { pagingData ->
                // 2. Usamos el ALIAS 'pagingDataMap' para transformar los items
                // Esto garantiza que el compilador sepa que 'pdf' es un PdfItem
                pagingData.pagingDataMap { pdf ->
                    pdf.copy(isFavorite = request.favoriteIds.contains(pdf.id))
                }
            }
    }.cachedIn(viewModelScope)



    data class DataRequest(val sort: PdfSortOption, val tab: PdfTab, val favoriteIds: Set<Long>)

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


    //Borrar PDF
    fun deletePdf(pdf: PdfItem){
        viewModelScope.launch(Dispatchers.IO){
            val success = repository.deletePdf(pdf.uri)
            if (success){
                repository.deleteFromRoom(pdf.id)
                _refreshTrigger.value+=1
            }else{
                withContext(Dispatchers.Main){
                    Toast.makeText(getApplication(), "No se pudo borrar el archivo", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    fun toggleFavorite(pdf: PdfItem, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(pdf, isFavorite)
            _isPdfFavorite.value = isFavorite
        }
    }


    // NUEVO: Renombrar PDF
    fun renamePdf(pdf: PdfItem, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Llamamos al repositorio pasando el callback 'onScanCompleted'
            val success = repository.renamePdf(
                uri = pdf.uri,
                newName = newName,
                onScanCompleted = {
                    // 2. Este bloque se ejecuta AUTOMÁTICAMENTE cuando Android termina de indexar.
                    // Incrementamos el trigger para recargar la lista al instante.
                    _refreshTrigger.value += 1
                }
            )

            // 3. Si falló el renombrado inicial (permisos, archivo no existe, etc), avisamos.
            if (!success) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "No se pudo renombrar el archivo", Toast.LENGTH_SHORT).show()
                }
            }
            // Nota: Si success es true, no hacemos nada más aquí; el callback se encargará de refrescar.
        }
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