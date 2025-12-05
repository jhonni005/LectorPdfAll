package com.zonadev.lectordocumentos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zonadev.lectordocumentos.data.model.PdfSortOption

@Composable
fun SortBottomSheetContent(
    selectedOption: PdfSortOption,
    onOptionSelected: (PdfSortOption) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Ordenar por",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            // Color adaptable (Negro en Light, Blanco en Dark)
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Lista de Opciones
        SortOptionRow("Última modificación (Reciente)", Icons.Default.History, selectedOption == PdfSortOption.DATE_DESC) { onOptionSelected(PdfSortOption.DATE_DESC) }
        SortOptionRow("Última modificación (Antiguo)", Icons.Default.AccessTime, selectedOption == PdfSortOption.DATE_ASC) { onOptionSelected(PdfSortOption.DATE_ASC) }
        SortOptionRow("Nombre (A-Z)", Icons.Default.SortByAlpha, selectedOption == PdfSortOption.NAME_ASC) { onOptionSelected(PdfSortOption.NAME_ASC) }
        SortOptionRow("Nombre (Z-A)", Icons.Default.SortByAlpha, selectedOption == PdfSortOption.NAME_DESC) { onOptionSelected(PdfSortOption.NAME_DESC) }
        SortOptionRow("Tamaño (Mayor)", Icons.Default.DataUsage, selectedOption == PdfSortOption.SIZE_DESC) { onOptionSelected(PdfSortOption.SIZE_DESC) }
        SortOptionRow("Tamaño (Menor)", Icons.Default.DataUsage, selectedOption == PdfSortOption.SIZE_ASC) { onOptionSelected(PdfSortOption.SIZE_ASC) }

        Spacer(modifier = Modifier.height(20.dp))

        // Botones
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Botón Cancelar (Gris adaptable)
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    // surfaceVariant es un gris suave perfecto para botones secundarios
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Text("Cancelar")
            }

            // Botón Aceptar (Rojo adaptable)
            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(
                   // containerColor = MaterialTheme.colorScheme.primary, // Rojo
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary // Blanco o Negro según contraste
                ),
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            ) {
                Text("Aceptar")
            }
        }
    }
}

@Composable
private fun SortOptionRow(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            // Icono gris adaptable
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            // Texto adaptable
            color = MaterialTheme.colorScheme.onSurface
        )

        CustomRadioButton(isSelected = isSelected)
    }
}

@Composable
private fun CustomRadioButton(isSelected: Boolean) {
    // Color activo (Rojo) y borde inactivo (Gris) adaptables
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveBorderColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .size(24.dp)
            .background(
                color = if (isSelected) activeColor else Color.Transparent,
                shape = CircleShape
            )
            .border(
                width = 2.dp,
                color = if (isSelected) activeColor else inactiveBorderColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                // El color del check debe ser el opuesto al primario (generalmente blanco)
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}