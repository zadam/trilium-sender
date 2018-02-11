package io.github.zadam.triliumsender

import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import io.github.zadam.triliumsender.services.TriliumSettings
import io.github.zadam.triliumsender.services.Utils
import kotlinx.android.synthetic.main.activity_login.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject


class LoginActivity : AppCompatActivity() {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private var loginTask: UserLoginTask? = null

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
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        if (loginTask != null) {
            return
        }

        // Reset errors.
        usernameEditText.error = null
        passwordEditText.error = null

        // Store values at the time of the login attempt.
        val triliumAddress = triliumAddressEditText.text.toString();
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid username
        if (TextUtils.isEmpty(username)) {
            usernameEditText.error = getString(R.string.error_field_required)
            focusView = usernameEditText
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            loginTask = UserLoginTask(triliumAddress, username, password)
            loginTask!!.execute(null as Void?)
        }
    }

    inner class LoginResult (val success: Boolean, val errorCode : Int?,
                             val token : String? = null);

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    inner class UserLoginTask internal constructor(private val triliumAddress: String, private val username: String, private val password: String) : AsyncTask<Void, Void, LoginResult>() {

        val TAG : String = "UserLoginTask"

        override fun doInBackground(vararg params: Void): LoginResult {

            val client = OkHttpClient()

            val json = JSONObject()
            json.put("username", username)
            json.put("password", password)

            val body = RequestBody.create(Utils.JSON, json.toString())
            val request = Request.Builder()
                    .url(triliumAddress + "/api/sender/login")
                    .post(body)
                    .build()

            val response: Response;

            try {
                response = client.newCall(request).execute()
            }
            catch (e: Exception) {
                Log.e(TAG, "Can't connect to Trilium server", e);

                return LoginResult(false, R.string.error_network_error)
            }

            Log.i(TAG,"Response code: " + response.code())

            if (response.code() == 401) {
                return LoginResult(false, R.string.error_incorrect_credentials)
            }
            else if (response.code() != 200) {
                return LoginResult(false, R.string.error_unexpected_response)
            }

            val responseText = response.body()?.string()

            Log.i(TAG,"Response text: " + responseText)

            val resp = JSONObject(responseText)

            val token : String = resp.get("token") as String

            Log.i(TAG,"Token: " + token)

            return LoginResult(true, null, token);
        }

        override fun onPostExecute(loginResult: LoginResult) {
            loginTask = null

            if (loginResult.success) {
                TriliumSettings(this@LoginActivity).save(triliumAddress, loginResult.token!!)

                Toast.makeText(this@LoginActivity, "Trilium connection settings have been successfully configured.", Toast.LENGTH_LONG).show()

                finish()
            } else {
                if (loginResult.errorCode == R.string.error_network_error
                    || loginResult.errorCode == R.string.error_unexpected_response) {

                    triliumAddressEditText.error = getString(loginResult.errorCode)
                    triliumAddressEditText.requestFocus()
                }
                else if (loginResult.errorCode == R.string.error_incorrect_credentials) {
                    passwordEditText.error = getString(loginResult.errorCode)
                    passwordEditText.requestFocus()
                }
                else {
                    throw RuntimeException("Unknown code: " + loginResult.errorCode);
                }
            }
        }

        override fun onCancelled() {
            loginTask = null
        }
    }
}
