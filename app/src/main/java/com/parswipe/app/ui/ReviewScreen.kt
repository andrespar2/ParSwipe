package com.parswipe.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.launch

@Composable
fun ReviewScreen(
    vm: ParSwipeViewModel,
    onApplyDeletions: (List<PhotoItem>) -> Unit,
    onRestoreLast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val current = vm.currentPhoto
    val next = vm.nextPhoto

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    var busy by remember { mutableStateOf(false) }

    val threshold = with(density) { 96.dp.toPx() }
    val flyDistance = with(density) { 900.dp.toPx() }

    suspend fun animateOut(delete: Boolean) {
        if (current == null || busy) return
        busy = true
        offsetX.animateTo(if (delete) flyDistance else -flyDistance, tween(190))
        vm.review(delete)
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
        busy = false
    }

    fun decide(delete: Boolean) {
        scope.launch { animateOut(delete) }
    }

    LaunchedEffect(current?.id, vm.photos.size) {
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
        busy = false
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "ParSwipe",
            subtitle = if (vm.photos.isEmpty()) {
                "Sin fotos para revisar"
            } else {
                "${vm.remaining} por revisar · ${vm.index} de ${vm.photos.size}"
            },
            trailing = {
                IconButton(onClick = { vm.loadPhotos() }) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Recargar",
                        tint = ParColors.TextPrimary,
                    )
                }
            },
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            when {
                vm.loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = ParColors.TextPrimary,
                )

                current == null -> EmptyState(
                    modifier = Modifier.align(Alignment.Center),
                    title = if (vm.photos.isEmpty()) "No hay fotos" else "¡Todo revisado!",
                    description = if (vm.photos.isEmpty()) {
                        "Concede el acceso a tus fotos para empezar a revisarlas."
                    } else {
                        "Has revisado las ${vm.photos.size} fotos disponibles."
                    },
                    action = {
                        ActionButton(
                            label = "Buscar fotos",
                            container = ParColors.SurfaceVariant,
                            contentColor = ParColors.TextPrimary,
                            enabled = true,
                            onClick = { vm.loadPhotos() },
                        )
                    },
                )

                else -> {
                    next?.let { following ->
                        PhotoCard(
                            photo = following,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = 0.94f
                                    scaleY = 0.94f
                                    alpha = 0.45f
                                },
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = offsetX.value
                                translationY = offsetY.value
                                rotationZ = (offsetX.value / 60f).coerceIn(-14f, 14f)
                            }
                            .pointerInput(current.id) {
                                detectDragGestures(
                                    onDragEnd = {
                                        when {
                                            offsetX.value > threshold -> decide(true)
                                            offsetX.value < -threshold -> decide(false)
                                            else -> scope.launch {
                                                offsetX.animateTo(0f, spring())
                                                offsetY.animateTo(0f, spring())
                                            }
                                        }
                                    },
                                    onDrag = { change, drag ->
                                        change.consume()
                                        scope.launch {
                                            offsetX.snapTo(offsetX.value + drag.x)
                                            offsetY.snapTo(offsetY.value + drag.y * 0.35f)
                                        }
                                    },
                                )
                            },
                    ) {
                        PhotoCard(photo = current, modifier = Modifier.fillMaxSize())

                        SwipeBadge(
                            text = "ELIMINAR",
                            color = ParColors.Delete,
                            alpha = (offsetX.value / threshold).coerceIn(0f, 1f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .rotate(-12f),
                        )
                        SwipeBadge(
                            text = "CONSERVAR",
                            color = ParColors.Keep,
                            alpha = (-offsetX.value / threshold).coerceIn(0f, 1f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .rotate(12f),
                        )
                    }
                }
            }
        }

        if (vm.lastApplied.isNotEmpty()) {
            RestoreBar(count = vm.lastApplied.size, onRestore = onRestoreLast)
        }

        if (vm.pendingDeletes.isNotEmpty()) {
            PendingBar(
                count = vm.pendingDeletes.size,
                bytes = vm.pendingDeletes.sumOf { it.sizeBytes },
                onApply = { onApplyDeletions(vm.pendingDeletes.toList()) },
            )
        }

        ActionRow(
            deleteEnabled = current != null,
            keepEnabled = current != null,
            canUndo = vm.canUndo,
            onDelete = { decide(true) },
            onKeep = { decide(false) },
            onUndo = { vm.undo() },
        )
    }
}

@Composable
private fun PhotoCard(photo: PhotoItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(ParColors.Black)
            .border(1.dp, ParColors.Outline, RoundedCornerShape(24.dp)),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.uri)
                .crossfade(true)
                .build(),
            contentDescription = photo.name,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(ParColors.Surface),
            contentScale = ContentScale.Fit,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = photo.name,
                color = ParColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(formatBytes(photo.sizeBytes))
                    if (photo.width > 0 && photo.height > 0) {
                        append(" · ${photo.width}×${photo.height}")
                    }
                    if (photo.bucket.isNotBlank()) append(" · ${photo.bucket}")
                },
                color = ParColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SwipeBadge(
    text: String,
    color: Color,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(24.dp)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(10.dp))
            .border(2.dp, color, RoundedCornerShape(10.dp))
            .background(ParColors.Black.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun PendingBar(count: Int, bytes: Long, onApply: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ParColors.Surface)
            .border(1.dp, ParColors.Outline, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.DeleteSweep,
            contentDescription = null,
            tint = ParColors.Delete,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$count en cola para eliminar",
                color = ParColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = formatBytes(bytes),
                color = ParColors.TextSecondary,
                fontSize = 11.sp,
            )
        }
        ActionButton(
            label = "Aplicar",
            container = ParColors.Delete,
            contentColor = ParColors.Black,
            enabled = true,
            onClick = onApply,
        )
    }
}

@Composable
private fun RestoreBar(count: Int, onRestore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ParColors.Surface)
            .border(1.dp, ParColors.Outline, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Restore,
            contentDescription = null,
            tint = ParColors.Keep,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "$count en la papelera de Android",
            color = ParColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            label = "Restaurar",
            container = ParColors.SurfaceVariant,
            contentColor = ParColors.TextPrimary,
            enabled = true,
            onClick = onRestore,
        )
    }
}

@Composable
private fun ActionRow(
    deleteEnabled: Boolean,
    keepEnabled: Boolean,
    canUndo: Boolean,
    onDelete: () -> Unit,
    onKeep: () -> Unit,
    onUndo: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onUndo,
            enabled = canUndo,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(ParColors.SurfaceVariant),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Undo,
                contentDescription = "Deshacer",
                tint = if (canUndo) ParColors.TextPrimary else ParColors.TextSecondary,
            )
        }

        ActionButton(
            label = "Eliminar",
            container = ParColors.Delete,
            contentColor = ParColors.Black,
            enabled = deleteEnabled,
            modifier = Modifier.weight(1f),
            onClick = onDelete,
        )

        ActionButton(
            label = "Conservar",
            container = ParColors.Keep,
            contentColor = ParColors.Black,
            enabled = keepEnabled,
            modifier = Modifier.weight(1f),
            onClick = onKeep,
        )
    }
}
