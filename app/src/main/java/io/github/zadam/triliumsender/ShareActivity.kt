package io.github.zadam.triliumsender

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import okhttp3.*
import okhttp3.internal.Util
import okio.BufferedSink
import okio.Okio
import okio.Source
import java.io.IOException
import java.io.InputStream


class ShareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)

        val prefs = this.getSharedPreferences(MainActivity.PREFRENCES_NAME, Context.MODE_PRIVATE);

        val triliumAddress = prefs.getString(MainActivity.PREF_TRILIUM_ADDRESS, "");
        val token = prefs.getString(MainActivity.PREF_TOKEN, "");

        if (triliumAddress.isBlank() || token.isBlank()) {
            Toast.makeText(this, "Trilium Sender is not configured. Can't sent the image.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val imageUri = intent.extras!!.get(Intent.EXTRA_STREAM) as Uri
        val mimeType = contentResolver.getType(imageUri)

        val sendImageTask = SendImageTask(imageUri, mimeType, triliumAddress, token)
        sendImageTask.execute(null as Void?)
    }

    inner class SendImageTask internal constructor(private val imageUri: Uri, private val mimeType: String,
                                                   private val triliumAddress: String, private val token: String) : AsyncTask<Void, Void, Boolean>() {

        val TAG : String = "SendImageTask"

        override fun doInBackground(vararg params: Void): Boolean {

            val imageStream = contentResolver.openInputStream(imageUri);

            val imageBody = RequestBodyUtil.create(MediaType.parse(mimeType)!!, imageStream)

            val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("upload", "image", imageBody)
                    .build()

            val client = OkHttpClient()

            val request = Request.Builder()
                    .url(triliumAddress + "/api/sender/image")
                    .addHeader("Authorization", token)
                    .post(requestBody)
                    .build()

            try {
                val response = client.newCall(request).execute()

                return response.code() == 200
            }
            catch (e: Exception) {
                Log.e(TAG, "Sending to Trilium failed", e)

                return false;
            }
        }

        override fun onPostExecute(result: Boolean) {
            if (result) {
                Toast.makeText(this@ShareActivity, "Image sent to Trilium", Toast.LENGTH_LONG).show()
            }
            else {
                Toast.makeText(this@ShareActivity, "Sending to Trilium failed", Toast.LENGTH_LONG).show()
            }

            finish()
        }

        override fun onCancelled() {
        }
    }


    object RequestBodyUtil {
        fun create(mediaType: MediaType, inputStream: InputStream): RequestBody {
            return object : RequestBody() {
                override fun contentType(): MediaType? {
                    return mediaType
                }

                override fun contentLength(): Long {
                    try {
                        return inputStream.available().toLong()
                    } catch (e: IOException) {
                        return 0
                    }
                }

                @Throws(IOException::class)
                override fun writeTo(sink: BufferedSink) {
                    var source: Source? = null
                    try {
                        source = Okio.source(inputStream)
                        sink.writeAll(source!!)
                    } finally {
                        Util.closeQuietly(source)
                    }
                }
            }
        }
    }
}
