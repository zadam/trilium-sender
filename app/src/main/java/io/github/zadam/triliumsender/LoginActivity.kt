package io.github.zadam.triliumsender

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.zadam.triliumsender.services.TriliumSettings
import io.github.zadam.triliumsender.services.Utils
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject


class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        passwordEditText.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })

        loginButton.setOnClickListener { attemptLogin() }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     *
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     *
     * If the login attempt errors out, some common errors are presented on the form.
     *
     * If the login attempt succeeds, the LoginActivity finishes.
     */
    private fun attemptLogin() {
        // Reset errors.
        triliumAddressEditText.error = null
        usernameEditText.error = null
        passwordEditText.error = null

        // Store values at the time of the login attempt.
        val triliumAddress = triliumAddressEditText.text.toString()
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()

        // Check for an empty URL. Flag and abort if so.
        if (TextUtils.isEmpty(triliumAddress)) {
            triliumAddressEditText.error = getString(R.string.error_field_required)
            triliumAddressEditText.requestFocus()
            return
        }

        // Check for a valid URL. Flag and abort if not.
        // Use the full address to the login API, for full coverage of the URL's validity.
        val fullTriliumAddress = "$triliumAddress/api/sender/login"
        val url = fullTriliumAddress.toHttpUrlOrNull()
        if (url == null) {
            triliumAddressEditText.error = getString(R.string.url_invalid)
            triliumAddressEditText.requestFocus()
            return
        }

        // Check for an empty username. Flag and abort if so.
        if (TextUtils.isEmpty(username)) {
            usernameEditText.error = getString(R.string.error_field_required)
            usernameEditText.requestFocus()
            return
        }

        // Check for an empty password. Flag and abort if so.
        if (TextUtils.isEmpty(password)) {
            passwordEditText.error = getString(R.string.error_field_required)
            passwordEditText.requestFocus()
            return
        }

        // Kick off a coroutine to handle the actual login attempt without blocking the UI.
        // Since we want to be able to fire Toasts, we should use the Main (UI) scope.
        val uiScope = CoroutineScope(Dispatchers.Main)
        uiScope.launch {

            val loginResult = doLogin(triliumAddress, username, password)

            if (loginResult.success) {
                // Store the address and api token.
                TriliumSettings(this@LoginActivity).save(triliumAddress, loginResult.token!!)
                // Announce our success.
                Toast.makeText(this@LoginActivity, getString(R.string.connection_configured_correctly), Toast.LENGTH_LONG).show()
                // End the activity.
                finish()
            } else {
                if (loginResult.errorCode == R.string.error_network_error
                        || loginResult.errorCode == R.string.error_unexpected_response) {

                    triliumAddressEditText.error = getString(loginResult.errorCode)
                    triliumAddressEditText.requestFocus()
                } else if (loginResult.errorCode == R.string.error_incorrect_credentials) {
                    passwordEditText.error = getString(loginResult.errorCode)
                    passwordEditText.requestFocus()
                } else {
                    throw RuntimeException("Unknown code: " + loginResult.errorCode)
                }
            }
        }
    }


    /**
     * A result from a login attempt.
     */
    inner class LoginResult(val success: Boolean, val errorCode: Int?,
                            val token: String? = null)

    /**
     * Makes the actual login http request in the IO thread, to avoid blocking the UI thread.
     *
     * @param triliumAddress, the base address of a Trilium server
     * @param username, the username to log into the server
     * @param password, the password to log into the server
     *
     * @return A loginResult object.
     */
    private suspend fun doLogin(triliumAddress: String, username: String, password: String): LoginResult {
        return withContext(Dispatchers.IO) {
            val tag = "UserLoginCoroutine"
            val client = OkHttpClient()

            val json = JSONObject()
            json.put("username", username)
            json.put("password", password)

            val body = json.toString().toRequestBody(Utils.JSON)
            val request = Request.Builder()
                    .url("$triliumAddress/api/sender/login")
                    .post(body)
                    .build()

            val response: Response

            try {
                // In the Dispatchers.IO context, blocking http requests are allowed.
                @Suppress("BlockingMethodInNonBlockingContext")
                response = client.newCall(request).execute()
            } catch (e: Exception) {
                Log.e(tag, "Can't connect to Trilium server", e)

                return@withContext LoginResult(false, R.string.error_network_error)
            }

            Log.i(tag, "Response code: " + response.code)

            if (response.code == 401) {
                return@withContext LoginResult(false, R.string.error_incorrect_credentials)
            } else if (response.code != 200) {
                return@withContext LoginResult(false, R.string.error_unexpected_response)
            }

            // In the Dispatchers.IO context, blocking tasks are allowed.
            @Suppress("BlockingMethodInNonBlockingContext")
            val responseText = response.body?.string()

            Log.i(tag, "Response text: $responseText")

            val resp = JSONObject(responseText!!)

            val token: String = resp.get("token") as String

            Log.i(tag, "Token: $token")

            return@withContext LoginResult(true, null, token)
        }
    }
}
