package com.zonadev.lectordocumentos.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat


object PermissionHelper {

    // 1️⃣ Verifica si ya tienes acceso a todos los archivos
    fun hasManageAllFiles(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ → revisa permiso especial
            Environment.isExternalStorageManager()
        } else {
            // Android 10 o anterior → usa READ_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    // 2️⃣ Intent seguro para Android 11+ (All Files Access)
    fun manageAllFilesIntentSafe(context: Context): Intent? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // ✅ Este es el que muestra el radiobutton de "Acceso a todos los archivos"
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                // 👉 Si es Huawei o Android < 11, lanzamos la solicitud del permiso normal
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // 3️⃣ Intent fallback: abrir configuración de la app
    fun appSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}


