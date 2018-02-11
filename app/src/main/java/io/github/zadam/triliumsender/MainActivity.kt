package io.github.zadam.triliumsender

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val PREFRENCES_NAME = "io.github.zadam.triliumsender";
        const val PREF_TRILIUM_ADDRESS = "trilium_address";
        const val PREF_TOKEN = "token";
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (false) {
            val prefs = getSharedPreferences(PREFRENCES_NAME, Context.MODE_PRIVATE);

            val editor = prefs.edit()
            editor.putString(PREF_TRILIUM_ADDRESS, "")
            editor.putString(PREF_TOKEN, "")
            editor.apply()
        }

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        setupConnectionButton.setOnClickListener {
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
        }

        setSetupStatus()

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
    }

    public override fun onResume() {  // After a pause OR at startup
        super.onResume()

        setSetupStatus()
    }

    private fun setSetupStatus() {
        val prefs = getSharedPreferences(PREFRENCES_NAME, Context.MODE_PRIVATE)

        val triliumAddress = prefs.getString(PREF_TRILIUM_ADDRESS, "")
        val token = prefs.getString(PREF_TOKEN, "")

        val setupStatus = findViewById<TextView>(R.id.setupStatusTextView);

        if (triliumAddress.isBlank() || token.isBlank()) {
            setupStatus.setText("Trilium connection setup isn't finished yet.");
        } else {
            setupStatus.setText("Trilium connection has been set up for address: " + triliumAddress + ". " +
                    "You can still change it by tapping the button below.");
        }
    }
}
