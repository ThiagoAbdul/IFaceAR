package com.example.imageclassificationlivefeed

import android.content.ContentValues.TAG
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.core.content.edit
import java.util.Locale

// Função para converter uma Location em string formatada
fun Location?.toText(): String {
    return if (this != null) {
        // Limitar as casas decimais para melhorar a legibilidade

        String.format(Locale.getDefault(), "(%.5f, %.5f)", latitude, longitude)
    } else {
        "Localização desconhecida ou inexistente"
    }
}
internal object SharedPreferenceUtil {

    const val KEY_ENABLED = "tracking_location"
    private const val PREFERENCE_FILE_KEY = "com.example.imageclassificationlivefeed.PREFERENCE_FILE_KEY"

    fun getLocationTrackingPref(context: Context): Boolean =
        context.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun saveLocationTrackingPref(context: Context, requestingLocationUpdates: Boolean) {
        context.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_ENABLED, requestingLocationUpdates)
            commit()  // Use commit() para garantir a persistência imediata
        }
    }
}
