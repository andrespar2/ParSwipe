package com.parswipe.app.data

import android.net.Uri

/** Una fotografía encontrada en el almacenamiento del dispositivo. */
data class PhotoItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val dateAddedSeconds: Long,
    val width: Int,
    val height: Int,
    val bucket: String,
    val mimeType: String,
)

/** Grupo de fotografías consideradas duplicadas entre sí. */
data class DuplicateGroup(
    val id: String,
    val items: List<PhotoItem>,
) {
    val sizeBytes: Long get() = items.firstOrNull()?.sizeBytes ?: 0L
    val wastedBytes: Long get() = sizeBytes * (items.size - 1).coerceAtLeast(0)
}
