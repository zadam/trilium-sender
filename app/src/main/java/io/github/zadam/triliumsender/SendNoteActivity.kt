package io.github.zadam.triliumsender

import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import io.github.zadam.triliumsender.services.HtmlConverter
import io.github.zadam.triliumsender.services.TriliumSettings
import io.github.zadam.triliumsender.services.Utils
import kotlinx.android.synthetic.main.activity_send_note.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

class SendNoteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_note)

        val settings = TriliumSettings(this)

        if (!settings.isConfigured()) {
            Toast.makeText(this, "Trilium Sender is not configured. Can't sent the image.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        sendNoteButton.setOnClickListener { view ->
            val sendImageTask = SendNoteTask(noteTitleEditText.text.toString(), noteContentEditText.text.toString(), settings.triliumAddress, settings.apiToken)
            sendImageTask.execute(null as Void?)
        }
    }

    inner class SendNoteTask internal constructor(private val noteTitle: String,
                                                  private val noteText: String,
                                                  private val triliumAddress: String,
                                                  private val apiToken: String) : AsyncTask<Void, Void, Boolean>() {

        val TAG : String = "SendNoteTask"

        override fun doInBackground(vararg params: Void): Boolean {
            val client = OkHttpClient()

            val json = JSONObject()
            json.put("title", noteTitle)
            json.put("content", HtmlConverter().convertPlainTextToHtml(noteText))

            val body = RequestBody.create(Utils.JSON, json.toString())

            val request = Request.Builder()
                    .url(triliumAddress + "/api/sender/note")
                    .addHeader("Authorization", apiToken)
                    .addHeader("X-Local-Date", Utils.localDateStr())
                    .post(body)
                    .build()

            return try {
                val response = client.newCall(request).execute()

                response.code() == 200
            } catch (e: Exception) {
                Log.e(TAG, "Sending to Trilium failed", e)

                false
            }
        }

        override fun onPostExecute(success: Boolean) {
            if (success) {
                Toast.makeText(this@SendNoteActivity, "Note sent to Trilium", Toast.LENGTH_LONG).show()
            }
            else {
                Toast.makeText(this@SendNoteActivity, "Sending note to Trilium failed", Toast.LENGTH_LONG).show()
            }

            finish()
        }

        override fun onCancelled() {
        }
    }
}
