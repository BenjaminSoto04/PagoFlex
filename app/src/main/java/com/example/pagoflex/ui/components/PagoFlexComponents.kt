package com.example.pagoflex.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.pagoflex.ui.theme.*

@Composable
fun PagoFlexCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier, shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = Blanco), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun PagoFlexPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = ButtonDefaults.buttonColors(containerColor = Primario, contentColor = Blanco)) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PagoFlexSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Text(text)
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextoOscuro)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = GrisDeshabilitado)
    }
}

@Composable
fun EmptyState(message: String) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = GrisDeshabilitado, style = MaterialTheme.typography.bodyMedium)
    }
}

