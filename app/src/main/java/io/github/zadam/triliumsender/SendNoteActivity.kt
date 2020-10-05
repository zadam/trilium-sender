package io.github.zadam.triliumsender

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.zadam.triliumsender.services.HtmlConverter
import io.github.zadam.triliumsender.services.TriliumSettings
import io.github.zadam.triliumsender.services.Utils
import kotlinx.android.synthetic.main.activity_send_note.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class SendNoteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_note)

        val settings = TriliumSettings(this)

        if (!settings.isConfigured()) {
            // We can't do anything useful if we're not configured. Abort out.
            Toast.makeText(this, getString(R.string.sender_not_configured_note), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        sendNoteButton.setOnClickListener {
            // Kick off a coroutine to handle the actual send attempt without blocking the UI.
            // Since we want to be able to fire Toasts, we should use the Main (UI) scope.
            val uiScope = CoroutineScope(Dispatchers.Main)
            uiScope.launch {
                val success = sendNote(noteTitleEditText.text.toString(), noteContentEditText.text.toString(), settings.triliumAddress, settings.apiToken)
                if (success) {
                    // Announce our success and end the activity.
                    Toast.makeText(this@SendNoteActivity, getString(R.string.sending_note_complete), Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    // Announce our failure.
                    // Do not end the activity, so the user may decide to copy/store their note's contents without it disappearing.
                    Toast.makeText(this@SendNoteActivity, getString(R.string.sending_note_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Attempts to send a note to the Trilium server.
     *
     * Runs in the IO thread, to avoid blocking the UI thread.
     *
     * @param noteTitle, the title of the proposed note.
     * @param noteText, the body of the proposed note.
     * @param triliumAddress, the address of the Trilium server to send this note to.
     * @param apiToken, the security token for communicating with the Trilium server.
     *
     * @return Success or failure, as a boolean.
     */
    private suspend fun sendNote(noteTitle: String, noteText: String, triliumAddress: String, apiToken: String): Boolean {
        val tag = "SendNoteCoroutine"

        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()

            val json = JSONObject()
            json.put("title", noteTitle)
            json.put("content", HtmlConverter().convertPlainTextToHtml(noteText))

            val body = json.toString().toRequestBody(Utils.JSON)

            val request = Request.Builder()
                    .url("$triliumAddress/api/sender/note")
                    .addHeader("Authorization", apiToken)
                    .addHeader("X-Local-Date", Utils.localDateStr())
                    .post(body)
                    .build()

            return@withContext try {
                // In the Dispatchers.IO context, blocking http requests are allowed.
                @Suppress("BlockingMethodInNonBlockingContext")
                val response = client.newCall(request).execute()

                response.code == 200
            } catch (e: Exception) {
                Log.e(tag, getString(R.string.sending_failed), e)

                false
            }

        }

    }

}
