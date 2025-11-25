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
import com.zonadev.lectordocumentos.core.PermissionHelper
import com.zonadev.lectordocumentos.data.model.PdfItem
import com.zonadev.lectordocumentos.data.repository.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PdfListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfRepository(application.applicationContext)
    private var appOpsListener: AppOpsManager.OnOpChangedListener? = null

    // String manual para evitar errores de compilación en algunas versiones de Android Studio
    private val OP_MANAGE_EXTERNAL_STORAGE = "android:manage_external_storage"

    data class PdfListUiState(
        val isLoading: Boolean = false,
        val pdfs: List<PdfItem> = emptyList(),
        val needsPermission: Boolean = false,
        val isPermissionSkipped: Boolean = false
    )

    private val _uiState = MutableStateFlow(PdfListUiState())
    val uiState: StateFlow<PdfListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val context = getApplication<Application>().applicationContext

        if (PermissionHelper.hasManageAllFiles(context)) {
            stopWatchingPermission()
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, needsPermission = false)
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

    fun requestManageAllFiles(activityContext: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startWatchingPermission()
                // Abrimos Settings en la misma pila (sin NEW_TASK)
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
                        // 1. Magia de limpieza de pila
                        bringAppToFront(context)

                        // 2. Refrescar UI
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

    // --- CORRECCIÓN CLAVE AQUÍ ---
    private fun bringAppToFront(context: Context) {
        try {
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.let {
                // Limpiamos flags previos para evitar conflictos
                it.flags = 0

                // FLAG_ACTIVITY_NEW_TASK: Obligatorio porque llamamos desde ApplicationContext (el ViewModel).
                // FLAG_ACTIVITY_CLEAR_TOP: ¡La solución! Busca tu app en la pila y DESTRUYE todo lo que esté encima (Settings).
                // FLAG_ACTIVITY_SINGLE_TOP: Evita que tu app se reinicie desde cero (usa onNewIntent si está viva).
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