package com.parswipe.app

import android.app.Application
import android.content.IntentSender
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.parswipe.app.data.DuplicateGroup
import com.parswipe.app.data.MediaActions
import com.parswipe.app.data.MediaStoreRepository
import com.parswipe.app.data.PhotoItem
import com.parswipe.app.data.StatsStore
import kotlinx.coroutines.launch

/** Una decisión tomada por el usuario sobre una foto. */
data class Decision(val photo: PhotoItem, val deleted: Boolean)

class ParSwipeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaStoreRepository(application)
    private val stats = StatsStore(application)

    /** Máximo de movimientos que se pueden deshacer. */
    val maxUndo: Int = 3

    val photos = mutableStateListOf<PhotoItem>()
    private val history = mutableStateListOf<Decision>()
    val pendingDeletes = mutableStateListOf<PhotoItem>()
    val duplicates = mutableStateListOf<DuplicateGroup>()

    var index by mutableStateOf(0)
        private set
    var loading by mutableStateOf(true)
        private set
    var scanningDuplicates by mutableStateOf(false)
        private set
    var reviewed by mutableStateOf(stats.reviewed)
        private set
    var deleted by mutableStateOf(stats.deleted)
        private set
    var freedBytes by mutableStateOf(stats.freedBytes)
        private set

    /** Lote enviado al sistema y pendiente de su confirmación. */
    private var pendingApply: List<PhotoItem> = emptyList()

    /** Último lote eliminado (permite restaurarlo desde la papelera). */
    var lastApplied by mutableStateOf<List<PhotoItem>>(emptyList())
        private set

    /** Mensajes cortos para la UI. */
    var message by mutableStateOf<String?>(null)
        private set

    val currentPhoto: PhotoItem? get() = photos.getOrNull(index)
    val nextPhoto: PhotoItem? get() = photos.getOrNull(index + 1)
    val remaining: Int get() = (photos.size - index).coerceAtLeast(0)
    val undoCount: Int get() = history.size
    val canUndo: Boolean get() = history.isNotEmpty()
    val canRestoreLast: Boolean get() = lastApplied.isNotEmpty() && MediaActions.isTrashAvailable()

    fun consumeMessage() {
        message = null
    }

    fun loadPhotos() {
        viewModelScope.launch {
            loading = true
            val loaded = repository.loadImages()
            photos.clear()
            photos.addAll(loaded)
            index = 0
            history.clear()
            pendingDeletes.clear()
            lastApplied = emptyList()
            loading = false
            Log.i(TAG, "Galería cargada: ${loaded.size} fotos")
            if (loaded.isEmpty()) {
                message = "No se encontraron fotos. Revisa los permisos de acceso."
            }
        }
    }

    /** Registra una decisión: `true` = eliminar, `false` = conservar. */
    fun review(delete: Boolean) {
        val photo = photos.getOrNull(index) ?: return
        if (delete) pendingDeletes.add(photo)
        history.add(Decision(photo, delete))
        while (history.size > maxUndo) {
            history.removeAt(0)
        }
        index += 1
        reviewed += 1
        stats.reviewed = reviewed
    }

    /** Deshace el último movimiento (hasta 3). */
    fun undo() {
        if (history.isEmpty()) return
        val last = history.removeAt(history.size - 1)
        if (last.deleted) {
            pendingDeletes.remove(last.photo)
        }
        if (index > 0) index -= 1
        if (reviewed > 0) reviewed -= 1
        stats.reviewed = reviewed
    }

    /**
     * Aplica el borrado de [items].
     * - API 30+: devuelve el [IntentSender] de la papelera del sistema (requiere confirmación).
     * - API <= 29: borra directamente y devuelve `null`.
     */
    fun requestDelete(items: List<PhotoItem>): IntentSender? {
        if (items.isEmpty()) return null
        pendingApply = items
        return if (MediaActions.isTrashAvailable()) {
            Log.i(TAG, "Solicitando papelera del sistema para ${items.size} fotos")
            MediaActions.trashRequest(getApplication(), items.map { it.uri })
        } else {
            val deletedCount = MediaActions.legacyDelete(getApplication(), items.map { it.uri })
            markApplied(items)
            Log.i(TAG, "Borrado permanente legacy: $deletedCount fotos")
            message = "Eliminadas $deletedCount fotos de forma permanente"
            null
        }
    }

    /** Resultado de la confirmación del sistema. */
    fun onDeleteResult(success: Boolean) {
        if (success) {
            val batch = pendingApply
            markApplied(batch)
            message = "Movidas a la papelera: ${batch.size}"
        } else {
            message = "Borrado cancelado"
        }
        pendingApply = emptyList()
    }

    private fun markApplied(items: List<PhotoItem>) {
        if (items.isEmpty()) return
        deleted += items.size
        freedBytes += items.sumOf { it.sizeBytes }
        stats.deleted = deleted
        stats.freedBytes = freedBytes
        lastApplied = items
        pendingDeletes.removeAll(items)
        history.removeAll { it.deleted && items.contains(it.photo) }
        Log.i(TAG, "Eliminación confirmada: ${items.size} fotos, ${items.sumOf { it.sizeBytes }} bytes")
        // Las fotos borradas dejan de estar disponibles en el mazo.
        val removedBehind = items.count { photos.indexOf(it) in 0 until index }
        photos.removeAll(items)
        index = (index - removedBehind).coerceIn(0, photos.size)
    }

    /** Restaura el último lote desde la papelera de Android. */
    fun restoreLastApplied(): IntentSender? {
        val batch = lastApplied
        if (batch.isEmpty() || !MediaActions.isTrashAvailable()) return null
        return MediaActions.untrashRequest(getApplication(), batch.map { it.uri })
    }

    fun onRestoreResult(success: Boolean) {
        if (success) {
            deleted = (deleted - lastApplied.size).coerceAtLeast(0)
            freedBytes = (freedBytes - lastApplied.sumOf { it.sizeBytes }).coerceAtLeast(0)
            stats.deleted = deleted
            stats.freedBytes = freedBytes
            lastApplied = emptyList()
            message = "Fotos restauradas desde la papelera"
            loadPhotos()
        } else {
            message = "Restauración cancelada"
        }
    }

    fun lastAppliedLabel(): String = "Restaurar ${lastApplied.size} fotos"

    fun scanDuplicates() {
        if (scanningDuplicates) return
        viewModelScope.launch {
            scanningDuplicates = true
            val found = repository.findDuplicates(photos.toList())
            duplicates.clear()
            duplicates.addAll(found)
            scanningDuplicates = false
            message = if (found.isEmpty()) {
                "No se encontraron duplicados"
            } else {
                "Duplicados encontrados: ${found.sumOf { it.items.size - 1 }}"
            }
        }
    }

    fun applyDeletionsFrom(selected: Collection<PhotoItem>) {
        requestDelete(selected.toList())
    }

    fun resetStats() {
        stats.reset()
        reviewed = 0
        deleted = 0
        freedBytes = 0
        message = "Estadísticas reiniciadas"
    }

    companion object {
        const val TAG = "ParSwipe"
    }
}
