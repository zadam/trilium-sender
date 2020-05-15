package io.github.zadam.triliumsender

import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import io.github.zadam.triliumsender.services.ImageConverter
import io.github.zadam.triliumsender.services.RequestBodyUtil
import io.github.zadam.triliumsender.services.TriliumSettings
import io.github.zadam.triliumsender.services.Utils
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.Request


class ShareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)

        val settings = TriliumSettings(this)

        if (!settings.isConfigured()) {
            Toast.makeText(this, "Trilium Sender is not configured. Can't sent the image.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val imageUri = intent.extras!!.get(Intent.EXTRA_STREAM) as Uri
        val mimeType = contentResolver.getType(imageUri)

        val sendImageTask = SendImageTask(imageUri, mimeType, settings.triliumAddress, settings.apiToken)
        sendImageTask.execute(null as Void?)
    }

    inner class SendImageResult (val success: Boolean, val contentLength: Long? = null)

    inner class SendImageTask internal constructor(private val imageUri: Uri,
                                                   private val mimeType: String,
                                                   private val triliumAddress: String,
                                                   private val apiToken: String) : AsyncTask<Void, Void, SendImageResult>() {

        val TAG : String = "SendImageTask"

        override fun doInBackground(vararg params: Void): SendImageResult {

            val (requestBody, contentLength) = buildRequestBody()

            val client = CustomTrustClient.getClient()

            val request = Request.Builder()
                    .url(triliumAddress + "/api/sender/image")
                    .addHeader("Authorization", apiToken)
                    .addHeader("X-Local-Date", Utils.localDateStr())
                    .post(requestBody)
                    .build()

            try {
                val response = client.newCall(request).execute()

                return SendImageResult(response.code() == 200, contentLength)
            }
            catch (e: Exception) {
                Log.e(TAG, "Sending to Trilium failed", e)

                return SendImageResult(false)
            }
        }

        private fun buildRequestBody(): Pair<MultipartBody, Long> {
            val imageStream = contentResolver.openInputStream(imageUri);
            val scaledImage = ImageConverter().scaleImage(imageStream, mimeType)

            val imageBody = RequestBodyUtil.create(MediaType.parse(mimeType)!!, scaledImage)

            val contentLength = imageBody.contentLength()

            val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("upload", "image", imageBody)
                    .build()
            return Pair(requestBody, contentLength)
        }


        override fun onPostExecute(result: SendImageResult) {
            if (result.success) {
                Toast.makeText(this@ShareActivity, "Image sent to Trilium (" + (result.contentLength!! / 1000) + " KB)", Toast.LENGTH_LONG).show()
            }
            else {
                Toast.makeText(this@ShareActivity, "Sending to Trilium failed", Toast.LENGTH_LONG).show()
            }

            finish()
        }

        override fun onCancelled() {
        }
    }
}
