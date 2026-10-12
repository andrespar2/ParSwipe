package com.parswipe.app.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/** Acceso de lectura al catálogo de imágenes del dispositivo vía MediaStore. */
class MediaStoreRepository(private val context: Context) {

    suspend fun loadImages(): List<PhotoItem> = withContext(Dispatchers.IO) {
        val projection = buildList {
            add(MediaStore.Images.Media._ID)
            add(MediaStore.Images.Media.DISPLAY_NAME)
            add(MediaStore.Images.Media.SIZE)
            add(MediaStore.Images.Media.DATE_ADDED)
            add(MediaStore.Images.Media.DATE_TAKEN)
            add(MediaStore.Images.Media.MIME_TYPE)
            add(MediaStore.Images.Media.WIDTH)
            add(MediaStore.Images.Media.HEIGHT)
            add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        }.toTypedArray()

        val selection = buildString {
            append("${MediaStore.Images.Media.SIZE} > 0")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                append(" AND ${MediaStore.MediaColumns.IS_PENDING} = 0")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                append(" AND ${MediaStore.MediaColumns.IS_TRASHED} = 0")
            }
        }

        val result = ArrayList<PhotoItem>(512)
        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val takenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val taken = if (takenCol >= 0) cursor.getLong(takenCol) else 0L
                    val added = cursor.getLong(addedCol)
                    result += PhotoItem(
                        id = id,
                        uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id,
                        ),
                        name = cursor.getString(nameCol) ?: "IMG_$id",
                        sizeBytes = cursor.getLong(sizeCol),
                        dateAddedSeconds = if (taken > 0) taken / 1000 else added,
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                        bucket = cursor.getString(bucketCol) ?: "",
                        mimeType = cursor.getString(mimeCol) ?: "image/*",
                    )
                }
            }
        } catch (_: SecurityException) {
            return@withContext emptyList()
        }
        result
    }

    /**
     * Detección de duplicados: primero por tamaño exacto (filtro barato) y
     * después por huella SHA-256 de los primeros 512 KB (sin cargar la imagen
     * completa en memoria). Todo el proceso es local.
     */
    suspend fun findDuplicates(items: List<PhotoItem>): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val groups = ArrayList<DuplicateGroup>()
        items.asSequence()
            .filter { it.sizeBytes > 0L }
            .groupBy { it.sizeBytes }
            .filter { it.value.size > 1 }
            .forEach { (size, candidates) ->
                candidates.groupBy { sampleHash(it) }.forEach { (hash, same) ->
                    if (hash != null && same.size > 1) {
                        groups += DuplicateGroup(
                            id = "${size}_$hash",
                            items = same.sortedBy { it.dateAddedSeconds },
                        )
                    }
                }
            }
        groups.sortedByDescending { it.wastedBytes }
    }

    private fun sampleHash(item: PhotoItem): String? = try {
        context.contentResolver.openInputStream(item.uri)?.use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            var remaining = 512 * 1024
            while (remaining > 0) {
                val read = input.read(buffer, 0, minOf(buffer.size, remaining))
                if (read <= 0) break
                digest.update(buffer, 0, read)
                remaining -= read
            }
            digest.update(item.sizeBytes.toString().toByteArray())
            digest.digest().joinToString("") { b ->
                (b.toInt() and 0xFF).toString(16).padStart(2, '0')
            }
        }
    } catch (_: Throwable) {
        null
    }
}
