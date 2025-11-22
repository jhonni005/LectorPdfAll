package com.zonadev.lectordocumentos.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

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
suspend fun renderPdfPage(
    renderer: PdfRenderer,
    index: Int,
    mutex: Mutex // 🔥 Necesario para evitar colisiones
): Bitmap? {
    return withContext(Dispatchers.IO) {
        // Usamos mutex para asegurar que solo UN hilo toque el PdfRenderer a la vez
        mutex.withLock {
            if (index >= renderer.pageCount) return@withLock null

            var page: PdfRenderer.Page? = null
            try {
                page = renderer.openPage(index)

                // Configuración para ahorrar memoria (50% menos que ARGB_8888)
                val bitmapConfig = Bitmap.Config.RGB_565

                val maxWidth = 900 // Ancho objetivo
                val scale = maxWidth.toFloat() / page.width
                val width = maxWidth
                val height = (page.height * scale).roundToInt()

                // Crear bitmap optimizado
                val bitmap = createBitmap(width, height, bitmapConfig)

                val matrix = Matrix().apply {
                    postScale(scale, scale)
                }

                page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap

            } catch (e: Exception) {
                e.printStackTrace()
                null // Retornar null si falla, para manejarlo en la UI
            } finally {
                page?.close() // 🔥 CRÍTICO: Siempre cerrar la página
            }
        }
    }
}

class BitmapCache(private val maxSize: Int) {
    private val map = LinkedHashMap<Int, Bitmap>(0, 0.75f, true)

    fun get(index: Int): Bitmap? = map[index]

    fun put(index: Int, bmp: Bitmap) {
        map[index] = bmp
        if (map.size > maxSize) {
            val iterator = map.entries.iterator()
            val oldest = iterator.next()
            iterator.remove()

            // ❌ NO RECICLAR, PdfRenderer puede seguir usándolo
            // oldest.value.recycle()  <- JAMÁS HACER ESTO
        }
    }
}
