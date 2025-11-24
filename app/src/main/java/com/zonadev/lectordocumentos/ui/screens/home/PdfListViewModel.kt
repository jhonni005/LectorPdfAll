package com.zonadev.lectordocumentos.ui.screens.home

import android.app.Application
import android.content.Context
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

    // Estado de la UI
    data class PdfListUiState(
        val isLoading: Boolean = false,
        val pdfs: List<PdfItem> = emptyList(),
        val needsPermission: Boolean = false,
        val isPermissionSkipped: Boolean = false // Nuevo estado
    )

    private val _uiState = MutableStateFlow(PdfListUiState())
    val uiState: StateFlow<PdfListUiState> = _uiState.asStateFlow()

    fun refresh() {
        val context = getApplication<Application>().applicationContext

        if (!PermissionHelper.hasManageAllFiles(context)) {
            _uiState.value = _uiState.value.copy(needsPermission = true, isLoading = false)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, needsPermission = false)

            val pdfs = withContext(Dispatchers.IO) { repository.listPdfs() }

            _uiState.value = _uiState.value.copy(isLoading = false, pdfs = pdfs)
        }
    }

    // Función para cuando el usuario presiona "Saltar"
    fun skipPermissionRequest() {
        _uiState.value = _uiState.value.copy(isPermissionSkipped = true)
    }

    fun requestManageAllFiles(activityContext: Context) {
        val intent = PermissionHelper.manageAllFilesIntentSafe(activityContext)
            ?: PermissionHelper.appSettingsIntent(activityContext)

        try {
            activityContext.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(activityContext, "Error al abrir configuración", Toast.LENGTH_LONG).show()
        }
    }
}