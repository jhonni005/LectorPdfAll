package com.zonadev.lectordocumentos.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.zonadev.lectordocumentos.data.model.PdfItem
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PdfDetailsDialog(
    pdf: PdfItem,
    onDismiss: () -> Unit
) {
    // Colores específicos solicitados (basados en tu tema oscuro)
    val dialogBackground = Color(0xFF1E1E1E) // SurfaceDark
    val titleColor = Color(0xFFE6E1E5)       // Blanco suave (OnSurfaceDark)
    val labelColor = Color(0xFFE6E1E5).copy(alpha = 0.8f) // Subtitulos un poco menos intensos
    val valueColor = Color(0xFFC4C7C5).copy(alpha = 0.7f) // Información más opaca
    val buttonRed = Color(0xFFD32F2F)        // Rojo marca

    // Formateamos los datos al iniciar el composable
    val formattedDate = remember(pdf.lastModified) { formatFullDate(pdf.lastModified) }
    val formattedSize = remember(pdf.size) { formatFileSize(pdf.size) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBackground),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Título Principal
                Text(
                    text = "Detalles",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // --- SECCIONES DE INFORMACIÓN ---
                DetailItem(label = "Título", value = pdf.name, labelColor, valueColor)
                Spacer(modifier = Modifier.height(16.dp))

                DetailItem(label = "Ruta", value = pdf.path ,labelColor, valueColor)
                Spacer(modifier = Modifier.height(16.dp))

                DetailItem(label = "Última modificación", value = formattedDate, labelColor, valueColor)
                Spacer(modifier = Modifier.height(16.dp))

                DetailItem(label = "Tamaño del archivo", value = formattedSize, labelColor, valueColor)

                Spacer(modifier = Modifier.height(32.dp))

                // Botón Aceptar
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Aceptar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium, // Estilo pequeño para el subtitulo
            fontWeight = FontWeight.SemiBold,
            color = labelColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium, // Estilo cuerpo para el valor
            color = valueColor
        )
    }
}

// --- FUNCIONES AUXILIARES DE FORMATO ---

// Formato completo: "23 ago 2024 11:33:02 a.m."
private fun formatFullDate(millis: Long): String {
    return try {
        val formatter = SimpleDateFormat("dd MMM yyyy HH:mm:ss a", Locale("es", "ES"))
        formatter.format(millis)
    } catch (e: Exception) {
        "Fecha desconocida"
    }
}

// Formato de tamaño (KB/MB)
private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format("%.2f MB", mb) // 2 decimales para más precisión en detalles
        else -> String.format("%.0f KB", kb)
    }
}