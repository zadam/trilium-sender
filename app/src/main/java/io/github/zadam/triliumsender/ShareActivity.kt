package io.github.zadam.triliumsender

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.zadam.triliumsender.services.ImageConverter
import io.github.zadam.triliumsender.services.RequestBodyUtil
import io.github.zadam.triliumsender.services.TriliumSettings
import io.github.zadam.triliumsender.services.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject


class ShareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)

        val settings = TriliumSettings(this)

        if (!settings.isConfigured()) {
            Toast.makeText(this, getString(R.string.sender_not_configured_image), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val imageUri = intent.extras!!.get(Intent.EXTRA_STREAM) as Uri
        val mimeType = contentResolver.getType(imageUri)!!

        // Kick off a coroutine to handle the actual image send without blocking the UI.
        // Since we want to be able to fire Toasts, we should use the Main (UI) scope.
        val uiScope = CoroutineScope(Dispatchers.Main)
        uiScope.launch {
            val result = doSendImage(imageUri, mimeType, settings.triliumAddress, settings.apiToken)
            if (result.success) {
                Toast.makeText(this@ShareActivity, getString(R.string.sending_image_complete_size, (result.contentLength!! / 1000)), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@ShareActivity, getString(R.string.sending_failed), Toast.LENGTH_LONG).show()
            }
            // Finish the activity either way- there's no editor UI here, so the user can't really lose data like on the Note sender.
            // They just need to attempt to re-share once they fix their connection.
            finish()
        }
    }

    /**
     * Given an input image, scale the image down to a maximum size, and build a request body for sending the scaled image.
     *
     * @param imageUri A URI to the image to build a request of.
     * @param mimeType The MIME type of the image, used in scaling the image.
     *
     * @return A pair, containing the multipart request body, and the contentLength of the request body.
     */
    private fun buildRequestBody(imageUri: Uri, mimeType: String): Pair<MultipartBody, Long> {
        val imageStream = contentResolver.openInputStream(imageUri)
        val scaledImage = ImageConverter().scaleImage(imageStream!!, mimeType)

        val imageBody = RequestBodyUtil.create(mimeType.toMediaType(), scaledImage)

        val contentLength = imageBody.contentLength()

        val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload", "image", imageBody)
                .build()
        return Pair(requestBody, contentLength)
    }

    /**
     * Attempts to send an image to the Trilium server. Runs in the IO thread, to avoid blocking the UI thread.
     *
     * @param imageUri, a URI for an image to send.
     * @param mimeType, the MIME type of the image to send.
     * @param triliumAddress, the address of the Trilium instance to send the image to.
     * @param apiToken, the API token for communicating with the Trilium instance.
     *
     * @return the result of the attempted image send.
     */
    private suspend fun doSendImage(imageUri: Uri, mimeType: String, triliumAddress: String, apiToken: String): SendImageResult {
        val settings = TriliumSettings(this)

        val labelList = JSONArray()

        if (settings.noteLabel.isNotEmpty()) {
            // The api actually supports a list of key-value pairs, but for now we just write one label.
            val label = JSONObject()
            label.put("name", settings.noteLabel)
            label.put("value", "")

            labelList.put(label)
        }

        return withContext(Dispatchers.IO) {
            val tag = "SendImageCoroutine"

            val (requestBody, contentLength) = buildRequestBody(imageUri, mimeType)

            val client = OkHttpClient()

            val request = Request.Builder()
                    .url("$triliumAddress/api/sender/image")
                    .addHeader("Authorization", apiToken)
                    .addHeader("X-Local-Date", Utils.localDateStr())
                    .addHeader("X-Labels", labelList.toString())
                    .post(requestBody)
                    .build()

            return@withContext try {

                // In the Dispatchers.IO context, blocking http requests are allowed.
                @Suppress("BlockingMethodInNonBlockingContext")
                val response = client.newCall(request).execute()

                SendImageResult(response.code == 200, contentLength)
            } catch (e: Exception) {
                Log.e(tag, getString(R.string.sending_failed), e)

                SendImageResult(false)
            }
        }
    }

    inner class SendImageResult(val success: Boolean, val contentLength: Long? = null)
}
