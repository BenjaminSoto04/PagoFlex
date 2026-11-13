package com.example.pagoflex.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pagoflex.ui.styles.BotonPrimario
import com.example.pagoflex.ui.styles.BotonSecundario
import com.example.pagoflex.ui.styles.EstilosTarjeta
import com.example.pagoflex.ui.theme.*

@Composable
fun PagoFlexCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    EstilosTarjeta.Tarjeta(modifier = modifier, content = content)
}

@Composable
fun PagoFlexPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    BotonPrimario(text = text, onClick = onClick, modifier = modifier, enabled = enabled)
}

@Composable
fun PagoFlexSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    BotonSecundario(text = text, onClick = onClick, modifier = modifier)
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EmptyState(message: String) {
    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}
