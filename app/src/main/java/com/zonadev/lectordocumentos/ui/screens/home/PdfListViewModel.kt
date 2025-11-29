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
import com.zonadev.lectordocumentos.core.PermissionHelper
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.data.model.PdfSortOption
import com.zonadev.lectordocumentos.data.repository.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PdfListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfRepository(application.applicationContext)
    private var appOpsListener: AppOpsManager.OnOpChangedListener? = null

    // Cadena manual para evitar error de referencia en algunas versiones de compilador
    private val OP_MANAGE_EXTERNAL_STORAGE = "android:manage_external_storage"

    data class PdfListUiState(
        val isLoading: Boolean = false,
        val pdfs: List<PdfItem> = emptyList(),
        val needsPermission: Boolean = false,
        val isPermissionSkipped: Boolean = false
    )

    private val _uiState = MutableStateFlow(PdfListUiState())
    val uiState: StateFlow<PdfListUiState> = _uiState.asStateFlow()

    // --- BÚSQUEDA ---
    private val _searchText = MutableStateFlow(TextFieldValue(""))
    val searchText = _searchText.asStateFlow()

    // --- ORDENAMIENTO ---
    private val _sortOption = MutableStateFlow(PdfSortOption.DATE_DESC)
    val sortOption = _sortOption.asStateFlow()

    // --- FLUJO 1: LISTA PRINCIPAL (Para PdfListScreen) ---
    // Siempre muestra TODOS los PDFs, solo aplica el ordenamiento.
    val sortedPdfs: StateFlow<List<PdfItem>> = combine(_uiState, _sortOption) { state, sort ->
        sortList(state.pdfs, sort)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- FLUJO 2: RESULTADOS DE BÚSQUEDA (Para PdfSearchScreen) ---
    // Depende del texto: Si está vacío devuelve lista vacía, si no, filtra y ordena.
    val searchResults: StateFlow<List<PdfItem>> = combine(_searchText, _uiState, _sortOption) { tfv, state, sort ->
        val text = tfv.text
        if (text.isBlank()) {
            emptyList() // <--- IMPORTANTE: Empieza vacío para no mostrar todo al abrir la lupa
        } else {
            // Filtramos y luego ordenamos los resultados
            val filtered = state.pdfs.filter { it.name.contains(text, ignoreCase = true) }
            sortList(filtered, sort)
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Función auxiliar para ordenar listas (evita repetir código)
    private fun sortList(list: List<PdfItem>, sort: PdfSortOption): List<PdfItem> {
        return when (sort) {
            PdfSortOption.DATE_DESC -> list.sortedByDescending { it.lastModified }
            PdfSortOption.DATE_ASC -> list.sortedBy { it.lastModified }
            PdfSortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            PdfSortOption.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
            PdfSortOption.SIZE_DESC -> list.sortedByDescending { it.size }
            PdfSortOption.SIZE_ASC -> list.sortedBy { it.size }
        }
    }

    fun onSearchTextChange(newValue: TextFieldValue) {
        _searchText.value = newValue
    }

    fun updateSortOption(newOption: PdfSortOption) {
        _sortOption.value = newOption
    }

    // --- INICIALIZACIÓN ---
    init {
        refresh()
    }

    fun refresh() {
        val context = getApplication<Application>().applicationContext

        if (PermissionHelper.hasManageAllFiles(context)) {
            stopWatchingPermission() // Ya tenemos permiso, dejamos de vigilar

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, needsPermission = false)

                // Carga de datos en hilo IO (Disco)
                val pdfs = withContext(Dispatchers.IO) { repository.listPdfs() }

                _uiState.value = _uiState.value.copy(isLoading = false, pdfs = pdfs)
            }
        } else {
            _uiState.value = _uiState.value.copy(needsPermission = true, isLoading = false)
        }
    }

    fun skipPermissionRequest() {
        _uiState.value = _uiState.value.copy(isPermissionSkipped = true)
        stopWatchingPermission()
    }

    // --- GESTIÓN DE PERMISOS ---
    fun requestManageAllFiles(activityContext: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // 1. Iniciamos vigilancia del sistema
                startWatchingPermission()

                // 2. Abrimos Settings SIN flag NEW_TASK para mantener la pila de navegación unida
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
            Toast.makeText(activityContext, "No se pudo abrir la configuración", Toast.LENGTH_LONG).show()
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

                        // B. Refrescar lista
                        viewModelScope.launch(Dispatchers.Main) {
                            refresh()
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