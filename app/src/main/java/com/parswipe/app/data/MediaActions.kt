package com.parswipe.app.data

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi

/**
 * Operaciones de borrado sobre el almacén multimedia.
 *
 * Android 11 (API 30) o superior: se usa la papelera del sistema
 * ([MediaStore.createTrashRequest]), con confirmación del usuario y recuperable
 * desde la galería. En versiones anteriores no existe papelera, por lo que el
 * borrado es definitivo y sólo se aplica tras confirmación explícita.
 */
object MediaActions {

    fun isTrashAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /** Pide al sistema mover a la papelera (recuperable). API 30+. */
    @RequiresApi(Build.VERSION_CODES.R)
    fun trashRequest(context: Context, uris: Collection<Uri>): IntentSender =
        MediaStore.createTrashRequest(context.contentResolver, uris, true).intentSender

    /** Pide al sistema sacar de la papelera. API 30+. */
    @RequiresApi(Build.VERSION_CODES.R)
    fun untrashRequest(context: Context, uris: Collection<Uri>): IntentSender =
        MediaStore.createTrashRequest(context.contentResolver, uris, false).intentSender

    /** Borrado directo (definitivo) para Android 10 o inferior. Devuelve cuántas se borraron. */
    fun legacyDelete(context: Context, uris: Collection<Uri>): Int {
        var deleted = 0
        uris.forEach { uri ->
            try {
                if (context.contentResolver.delete(uri, null, null) > 0) deleted++
            } catch (_: Throwable) {
                // Se ignora el fallo individual y se continúa con el resto.
            }
        }
        return deleted
    }
}
