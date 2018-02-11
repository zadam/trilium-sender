package io.github.zadam.triliumsender.services

import okhttp3.MediaType
import java.text.SimpleDateFormat
import java.util.*

class Utils {
    companion object {
        val JSON = MediaType.parse("application/json; charset=utf-8")

        fun localDateStr(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            val date = dateFormat.format(Calendar.getInstance().getTime())

            return date!!
        }
    }
}