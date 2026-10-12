package com.parswipe.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.parswipe.app.ParSwipeViewModel
import com.parswipe.app.data.PhotoItem
import com.parswipe.app.data.formatBytes
import com.parswipe.app.ui.theme.ParColors

@Composable
fun DuplicatesScreen(
    vm: ParSwipeViewModel,
    onDeleteSelected: (List<PhotoItem>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = remember { mutableStateListOf<Long>() }

    LaunchedEffect(vm.duplicates.size, vm.duplicates.firstOrNull()?.id) {
        selected.clear()
        vm.duplicates.forEach { group ->
            group.items.drop(1).forEach { selected.add(it.id) }
        }
    }

    val selectedPhotos = vm.duplicates
        .flatMap { it.items }
        .filter { selected.contains(it.id) }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Duplicadas",
            subtitle = when {
                vm.scanningDuplicates -> "Analizando…"
                vm.duplicates.isEmpty() -> "Sin resultados"
                else -> "${vm.duplicates.size} grupos · " +
                    "${vm.duplicates.sumOf { it.items.size - 1 }} repetidas"
            },
            trailing = {
                IconButton(onClick = { vm.scanDuplicates() }) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Buscar duplicados",
                        tint = ParColors.TextPrimary,
                    )
                }
            },
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                vm.scanningDuplicates -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = ParColors.TextPrimary,
                )

                vm.duplicates.isEmpty() -> EmptyState(
                    modifier = Modifier.align(Alignment.Center),
                    title = "Sin duplicados detectados",
                    description = "Se comparan las fotos por tamaño y huella de contenido, " +
                        "todo de forma local en tu dispositivo.",
                    action = {
                        ActionButton(
                            label = "Analizar galería",
                            container = ParColors.SurfaceVariant,
                            contentColor = ParColors.TextPrimary,
                            enabled = vm.photos.isNotEmpty(),
                            onClick = { vm.scanDuplicates() },
                        )
                    },
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(vm.duplicates, key = { it.id }) { group ->
                        DuplicateCard(
                            group = group,
                            selected = selected,
                            onToggle = { photo ->
                                if (selected.contains(photo.id)) selected.remove(photo.id)
                                else selected.add(photo.id)
                            },
                            onDeleteGroup = { itemsToDelete ->
                                onDeleteSelected(itemsToDelete)
                            },
                        )
                    }
                }
            }
        }

        if (selectedPhotos.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionButton(
                    label = "Eliminar ${selectedPhotos.size} duplicadas " +
                        "(${formatBytes(selectedPhotos.sumOf { it.sizeBytes })})",
                    container = ParColors.Delete,
                    contentColor = ParColors.Black,
                    enabled = true,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onDeleteSelected(selectedPhotos) },
                )
            }
        }
    }
}

@Composable
private fun DuplicateCard(
    group: com.parswipe.app.data.DuplicateGroup,
    selected: List<Long>,
    onToggle: (PhotoItem) -> Unit,
    onDeleteGroup: (List<PhotoItem>) -> Unit,
) {
    val groupSelection = group.items.filter { selected.contains(it.id) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ParColors.Surface)
            .border(1.dp, ParColors.Outline, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "${group.items.size} copias · ${formatBytes(group.sizeBytes)} cada una",
            color = ParColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "Se puede liberar ${formatBytes(group.wastedBytes)}",
            color = ParColors.TextSecondary,
            fontSize = 11.sp,
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(group.items, key = { it.id }) { photo ->
                val isSelected = selected.contains(photo.id)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(110.dp)
                        .clickable { onToggle(photo) },
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ParColors.SurfaceVariant)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) ParColors.Delete else ParColors.Outline,
                                shape = RoundedCornerShape(14.dp),
                            ),
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(photo.uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = photo.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        Icon(
                            imageVector = if (isSelected) {
                                Icons.Rounded.CheckCircle
                            } else {
                                Icons.Rounded.RadioButtonUnchecked
                            },
                            contentDescription = null,
                            tint = if (isSelected) ParColors.Delete else ParColors.TextSecondary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(18.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = photo.name,
                        color = ParColors.TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        ActionButton(
            label = if (groupSelection.isEmpty()) {
                "Nada seleccionado"
            } else {
                "Eliminar ${groupSelection.size} de este grupo"
            },
            container = ParColors.SurfaceVariant,
            contentColor = ParColors.TextPrimary,
            enabled = groupSelection.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            onClick = { onDeleteGroup(groupSelection) },
        )
    }
}
