package com.zonadev.lectordocumentos.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zonadev.lectordocumentos.R

@Composable
fun PermissionCard(
    onManageAllFiles: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Texto "Saltar" en la esquina superior derecha
            Text(
                text = "Saltar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clickable { onSkip() }
                    .padding(16.dp)
            )

            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Imagen SVG (file_search)
                Image(
                    painter = painterResource(id = R.drawable.file_search),
                    contentDescription = "Permiso requerido",
                    modifier = Modifier.size(120.dp)
                )

                Spacer(Modifier.height(16.dp))

                // Título
                Text(
                    text = "Se requiere permiso",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(Modifier.height(8.dp))

                // Descripción
                Text(
                    text = "Para leer todos los archivos PDF permita que la aplicación acceda a todos los archivos.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )

                Spacer(Modifier.height(24.dp))

                // Botón Rojo
                Button(
                    onClick = onManageAllFiles,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Permitir")
                }
            }
        }
    }
}