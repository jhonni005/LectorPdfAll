package com.zonadev.lectordocumentos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize

import androidx.compose.material3.Surface

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.zonadev.lectordocumentos.navigation.AppNavHost

import com.zonadev.lectordocumentos.ui.theme.LectorDocumentosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LectorDocumentosTheme {
                val navController = rememberNavController()
                Surface(modifier = Modifier) {
                  //  PdfAppEntry
                    AppNavHost(navController = navController)
                }
            }
        }
    }
}

