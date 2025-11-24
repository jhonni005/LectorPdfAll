import android.graphics.Bitmap
import io.legere.pdfiumandroid.PdfiumCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext


// --- 1. Función de renderizado usando PDFIUM ---
suspend fun renderPdfPageWithPdfium(
    pdfiumCore: PdfiumCore,
    pdfDoc: io.legere.pdfiumandroid.PdfDocument,
    index: Int,
    mutex: Mutex
): Bitmap? {
    return withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                // Abrir página
                pdfiumCore.openPage(pdfDoc, index)

                // Calcular dimensiones (Pdfium usa puntos, no pixeles directos, pero es similar)
                val pageWidth = pdfiumCore.getPageWidthPoint(pdfDoc, index)
                val pageHeight = pdfiumCore.getPageHeightPoint(pdfDoc, index)

                // Definir ancho deseado (calidad)
                val targetWidth = 1080 // Calidad HD
                val scale = targetWidth.toFloat() / pageWidth
                val targetHeight = (pageHeight * scale).toInt()

                // Crear Bitmap (RGB_565 sigue siendo mejor para memoria)
                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565)

                // 🔥 RENDERIZADO CON PDFIUM
                // renderPageBitmap(bitmap, doc, index, startX, startY, drawSizeX, drawSizeY, renderAnnot)
                pdfiumCore.renderPageBitmap(
                    pdfDoc,
                    bitmap,
                    index,
                    0, 0,
                    targetWidth, targetHeight,
                    true // Renderizar anotaciones si las hay
                )

                // Importante: Cerrar la página en Pdfium libera memoria nativa,
                // pero NO cierra el documento entero.
                // En PdfiumAndroid no siempre es obligatorio cerrar cada página individualmente
                // como en el nativo, pero es buena práctica si la librería lo expone.
                // (La implementación estándar de PdfiumCore maneja esto internamente mejor).

                return@withLock bitmap

            } catch (e: Exception) {
                e.printStackTrace()
                return@withLock null
            }
        }
    }
}
