package io.github.zadam.triliumsender.services

import android.annotation.SuppressLint
import okhttp3.MediaType.Companion.toMediaType
import java.text.SimpleDateFormat
import java.util.*

class Utils {
    companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()

        @SuppressLint("SimpleDateFormat") // Android Studio wants us to use locale-specific date formats.
        fun localDateStr(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            return dateFormat.format(Calendar.getInstance().time)
        }
    }
}
