package com.parswipe.app.data

import android.content.Context

/** Estadísticas acumuladas, persistidas localmente (sin red). */
class StatsStore(context: Context) {

    private val prefs = context.getSharedPreferences("parswipe_stats", Context.MODE_PRIVATE)

    var reviewed: Long
        get() = prefs.getLong(KEY_REVIEWED, 0L)
        set(value) = prefs.edit().putLong(KEY_REVIEWED, value).apply()

    var deleted: Long
        get() = prefs.getLong(KEY_DELETED, 0L)
        set(value) = prefs.edit().putLong(KEY_DELETED, value).apply()

    var freedBytes: Long
        get() = prefs.getLong(KEY_FREED, 0L)
        set(value) = prefs.edit().putLong(KEY_FREED, value).apply()

    fun reset() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_REVIEWED = "reviewed"
        const val KEY_DELETED = "deleted"
        const val KEY_FREED = "freed_bytes"
    }
}
