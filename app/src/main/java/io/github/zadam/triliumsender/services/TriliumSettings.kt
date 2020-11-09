package io.github.zadam.triliumsender.services

import android.app.Activity
import android.content.Context

class TriliumSettings constructor(ctx: Activity) {
    companion object {
        const val PREF_NAME = "io.github.zadam.triliumsender.setup"
        const val PREF_TRILIUM_ADDRESS = "trilium_address"
        const val PREF_API_TOKEN = "api_token"
        const val PREF_NOTE_LABEL = "trilium_note_label"
    }

    fun save(triliumAddress: String, apiToken: String, noteLabel: String) {
        val editor = prefs.edit()
        editor.putString(PREF_TRILIUM_ADDRESS, triliumAddress)
        editor.putString(PREF_API_TOKEN, apiToken)
        editor.putString(PREF_NOTE_LABEL, noteLabel)
        editor.apply()
    }

    private val prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    val triliumAddress
        get() = prefs.getString(PREF_TRILIUM_ADDRESS, "")!!

    val apiToken
        get() = prefs.getString(PREF_API_TOKEN, "")!!

    val noteLabel
        get() = prefs.getString(PREF_NOTE_LABEL, "")!!

    fun isConfigured() = !triliumAddress.isBlank() && !apiToken.isBlank()
}
