package com.zonadev.lectordocumentos.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.zonadev.lectordocumentos.model.PdfItem
import com.zonadev.lectordocumentos.utils.PdfRepository
import com.zonadev.lectordocumentos.utils.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PdfListPresenter(
    private val context: Context,
    private val repository: PdfRepository = PdfRepository(context)
) {

    interface View {
        fun showLoading()
        fun showPdfs(pdfs: List<PdfItem>)
        fun showPermissionRequired()
    }

    private var view: View? = null
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun attach(view: View) {
        this.view = view
        refresh()
    }

    fun detach() {
        coroutineScope.cancel()
        view = null
    }

    fun refresh() {
        view?.showLoading()
        coroutineScope.launch {
            if (PermissionHelper.hasManageAllFiles(context)) {
                val pdfs = withContext(Dispatchers.IO) { repository.listPdfs() }
                view?.showPdfs(pdfs)
            } else {
                view?.showPermissionRequired()
            }
        }
    }


    fun requestManageAllFiles() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // 📱 Huawei u otros Android 10 o menor
            val activity = context as? Activity ?: return
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                101
            )
            return
        }

        // 📲 Android 11+
        val intent = PermissionHelper.manageAllFilesIntentSafe(context)
            ?: PermissionHelper.appSettingsIntent(context)

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir la configuración de permisos", Toast.LENGTH_LONG).show()
        }
    }

}
