package io.github.zadam.triliumsender

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
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
import org.json.JSONArray
import org.json.JSONObject

class SendNoteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val tag = "SendNoteOnCreateHandler"
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_note)

        val settings = TriliumSettings(this)
        if (!settings.isConfigured()) {
            // We can't do anything useful if we're not configured. Abort out.
            Toast.makeText(this, getString(R.string.sender_not_configured_note), Toast.LENGTH_LONG).show()
            finish()
            return
        }
        if (settings.noteLabel.isNotEmpty()) {
            // We have a label to apply to this note! Indicate in the UI.
            labelList.text = getString(R.string.label_preview_template, settings.noteLabel)
        } else {
            // Hide the label text preview.
            labelList.visibility = View.GONE
        }

        // If we're a share-intent, pre-populate the note.
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    handleSendText(intent)
                } else {
                    // We don't yet have text/html support.
                    Log.e(tag, "Don't have a handler for share type ${intent.type}.")
                }
            }
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
     * If we're the target of a SEND intent, pre-fill the note's contents.
     *
     * @param intent, the Intent object, already verified to be of type text/plain.
     */
    private fun handleSendText(intent: Intent) {
        val tag = "SendNoteHandleSendText"

        // Many apps will suggest a "Text Subject", as in an email.
        val suggestedSubject = intent.getStringExtra((Intent.EXTRA_SUBJECT))
        if (suggestedSubject != null && suggestedSubject.isNotEmpty()) {
            // Use suggested subject.
            noteTitleEditText.setText(suggestedSubject, TextView.BufferType.EDITABLE)
        } else {
            // Try to craft a sane default title.
            var referrerName = "Android"

            // SDKs greater than 23 can access the referrer's package URI...
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                val referrer = this.referrer
                if (referrer != null && referrer.host != null) {
                    // Which we can use to get an AppInfo handle...
                    try {
                        // Which we can use to get an app label!
                        val referrerInfo = packageManager.getApplicationInfo(referrer.host.toString(), 0)
                        val potentialReferrerName = referrerInfo.loadLabel(packageManager).toString()
                        // Sanity check: is it an empty string?
                        if (potentialReferrerName.isNotEmpty()) {
                            referrerName = potentialReferrerName
                        }
                    } catch (e: PackageManager.NameNotFoundException) {
                        // Oh well, we have the default name to fall back on.
                        Log.w(tag, "Could not find package label for package name ${referrer.host}")
                    }
                }
            }
            // Ultimately, set the note title.
            noteTitleEditText.setText(getString(R.string.share_note_title, referrerName), TextView.BufferType.EDITABLE)
        }
        // And populate the note body!
        noteContentEditText.setText(intent.getStringExtra(Intent.EXTRA_TEXT), TextView.BufferType.EDITABLE)
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
        val settings = TriliumSettings(this)

        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()

            val json = JSONObject()

            if (noteTitle.isEmpty()) {
                // API does not allow empty note titles, so use a default value if the user doesn't set one.
                json.put("title", "Note from Android")
            } else {
                json.put("title", noteTitle)
            }
            json.put("content", HtmlConverter().convertPlainTextToHtml(noteText))

            if (settings.noteLabel.isNotEmpty()) {
                // The api actually supports a list of key-value pairs, but for now we just write one label.
                val label = JSONObject()
                label.put("name", settings.noteLabel)
                label.put("value", "")
                val labelList = JSONArray()
                labelList.put(label)
                json.put("labels", labelList)
            }

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
