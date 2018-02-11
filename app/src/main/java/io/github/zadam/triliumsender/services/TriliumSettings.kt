package io.github.zadam.triliumsender.services

import android.app.Activity
import android.content.Context

class TriliumSettings constructor(private val ctx: Activity) {
    companion object {
        const val PREF_NAME = "io.github.zadam.triliumsender.setup";
        const val PREF_TRILIUM_ADDRESS = "trilium_address";
        const val PREF_API_TOKEN = "api_token";
    }

    fun save(triliumAddress: String, apiToken: String) {
        val editor = prefs.edit()
        editor.putString(PREF_TRILIUM_ADDRESS, triliumAddress)
        editor.putString(PREF_API_TOKEN, apiToken);
        editor.apply()
    }

    private val prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    val triliumAddress: String
        get() = prefs.getString(PREF_TRILIUM_ADDRESS, "")

    val apiToken: String
        get() = prefs.getString(PREF_API_TOKEN, "")

    fun isConfigured() = !triliumAddress.isBlank() && !apiToken.isBlank()
}