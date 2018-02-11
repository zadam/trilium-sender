package io.github.zadam.triliumsender

import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_send_note.*
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

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
            val sendImageTask = SendNoteTask(noteTitle.text.toString(), noteText.text.toString(), settings.triliumAddress, settings.apiToken)
            sendImageTask.execute(null as Void?)
        }
    }

    inner class SendNoteTask internal constructor(private val noteTitle: String,
                                                  private val noteText: String,
                                                  private val triliumAddress: String,
                                                  private val apiToken: String) : AsyncTask<Void, Void, Boolean>() {

        val TAG : String = "SendNoteTask"
        val JSON = MediaType.parse("application/json; charset=utf-8")

        override fun doInBackground(vararg params: Void): Boolean {
            val client = OkHttpClient()

            val json = JSONObject()
            json.put("title", noteTitle)
            json.put("content", escape(noteText))

            val body = RequestBody.create(JSON, json.toString())

            val request = Request.Builder()
                    .url(triliumAddress + "/api/sender/note")
                    .addHeader("Authorization", apiToken)
                    .addHeader("X-Local-Date", now())
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

        private fun now(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            val date = dateFormat.format(Calendar.getInstance().getTime())

            return date!!
        }

        private fun escape(s: String): String {
            val builder = StringBuilder()
            var previousWasASpace = false
            for (c in s.toCharArray()) {
                if (c == ' ') {
                    if (previousWasASpace) {
                        builder.append("&nbsp;")
                        previousWasASpace = false
                        continue
                    }
                    previousWasASpace = true
                } else {
                    previousWasASpace = false
                }
                when (c) {
                    '<' -> builder.append("&lt;")
                    '>' -> builder.append("&gt;")
                    '&' -> builder.append("&amp;")
                    '"' -> builder.append("&quot;")
                    '\n' -> builder.append("<p>")
                    // We need Tab support here, because we print StackTraces as HTML
                    '\t' -> builder.append("&nbsp; &nbsp; &nbsp;")
                    else -> if (c.toInt() < 128) {
                        builder.append(c)
                    } else {
                        builder.append("&#").append(c.toInt()).append(";")
                    }
                }
            }
            return builder.toString()
        }

        override fun onCancelled() {
        }
    }
}
