package com.zonadev.lectordocumentos.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun PdfRow(
    namePdf: String,
    pdfDetails: String, // Recibe el texto ya listo ("12 mar - 4MB")
    onClick: () -> Unit,
    onMoreOptionsClick: () -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // OPTIMIZACIÓN: Clickable simple. Es más ligero que gestionar estados de interacción manuales.
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Icono PDF (Rojo estático)
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null, // null mejora el rendimiento de accesibilidad en scroll rápido
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 2. Información
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = namePdf,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // --- RENDIMIENTO MÁXIMO ---
                // Aquí solo pintamos el texto 'pdfDetails'.
                // Como el cálculo pesado (fechas/bytes) ya se hizo en el PagingSource (hilo secundario),
                // la UI vuela porque no tiene que pensar, solo mostrar.
                Text(
                    text = pdfDetails,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            // 3. Menú de opciones
            IconButton(onClick = onMoreOptionsClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opciones",
                    tint = Color.Gray
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = Color.LightGray.copy(alpha = 0.5f)
        )
    }
}