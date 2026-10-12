package com.parswipe.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parswipe.app.ParSwipeViewModel
import com.parswipe.app.data.MediaActions
import com.parswipe.app.data.formatBytes
import com.parswipe.app.ui.theme.ParColors

@Composable
fun StatsScreen(
    vm: ParSwipeViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = "Estadísticas",
            subtitle = "Actividad acumulada en este dispositivo",
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(
                label = "Fotos revisadas",
                value = vm.reviewed.toString(),
                accent = ParColors.TextPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = "Eliminadas",
                    value = vm.deleted.toString(),
                    accent = ParColors.Delete,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "Liberado",
                    value = formatBytes(vm.freedBytes),
                    accent = ParColors.Keep,
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = "En Android 11 o superior el espacio se recupera definitivamente " +
                    "cuando el sistema vacía su papelera.",
                color = ParColors.TextSecondary,
                fontSize = 11.sp,
            )

            DarkCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow("Pendientes en cola", vm.pendingDeletes.size.toString())
                    InfoRow("Fotos en el mazo", vm.photos.size.toString())
                    InfoRow("Duplicados encontrados", vm.duplicates.sumOf { it.items.size - 1 }.toString())
                    InfoRow(
                        "Papelera de Android",
                        if (MediaActions.isTrashAvailable()) "Disponible" else "No disponible",
                        highlight = MediaActions.isTrashAvailable(),
                    )
                    InfoRow("Procesamiento", "100% local, sin internet")
                }
            }

            ActionButton(
                label = "Reiniciar estadísticas",
                container = ParColors.SurfaceVariant,
                contentColor = ParColors.TextPrimary,
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = { vm.resetStats() },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = ParColors.TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = if (highlight) ParColors.Keep else ParColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
