package com.zonadev.lectordocumentos.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DeletePdfDialog(
    pdfName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    // Colores del tema (Hardcoded para el diseño oscuro solicitado, o usa MaterialTheme)
    val containerColor = Color(0xFF1E1E1E) // Fondo oscuro
    val titleColor = Color(0xFFE6E1E5)       // Blanco suave
    val bodyColor = Color(0xFFC4C7C5)        // Gris claro
    val deleteRed = Color(0xFFD32F2F)        // Rojo de peligro

    AlertDialog(
        onDismissRequest = onDismiss,
        // PARÁMETROS CORRECTOS MATERIAL 3:
        containerColor = containerColor,
        titleContentColor = titleColor,
        textContentColor = bodyColor,
        shape = RoundedCornerShape(16.dp),

        title = {
            Text(
                text = "Borrar archivo",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "¿Estás seguro de que quieres borrar \"$pdfName\"? Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = deleteRed,
                    contentColor = Color.White
                )
            ) {
                Text("Borrar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White.copy(alpha = 0.7f)
                )
            ) {
                Text("Cancelar")
            }
        }
    )
}