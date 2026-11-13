package com.example.pagoflex.ui.styles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tarjetas compartidas. El contenido se centra por defecto y usa un espaciado
 * compacto para evitar bloques con demasiado aire entre datos relacionados.
 */
object EstilosTarjeta {

    val rellenoEstandar = 16.dp
    val separacionEstandar = 8.dp

    @Composable
    fun Tarjeta(
        modifier: Modifier = Modifier,
        centrada: Boolean = true,
        color: Color = MaterialTheme.colorScheme.surface,
        elevacion: Dp = 2.dp,
        relleno: PaddingValues = PaddingValues(rellenoEstandar),
        separacion: Dp = separacionEstandar,
        onClick: (() -> Unit)? = null,
        content: @Composable ColumnScope.() -> Unit
    ) {
        val clickModifier = if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        }

        Card(
            modifier = modifier.fillMaxWidth().then(clickModifier),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = color),
            elevation = CardDefaults.cardElevation(defaultElevation = elevacion)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(relleno),
                horizontalAlignment = if (centrada) Alignment.CenterHorizontally else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(separacion),
                content = content
            )
        }
    }
}
