package io.github.zadam.triliumsender

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import io.github.zadam.triliumsender.services.TriliumSettings

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    // to reset the application to uninitialized state, only for dev/testing purposes
    private val resetSetup = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (resetSetup) {
            TriliumSettings(this).save("", "")
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
        val settings = TriliumSettings(this)

        if (!settings.isConfigured()) {
            setupStatusTextView.text = "Trilium connection setup isn't finished yet.";
        } else {
            setupStatusTextView.text = "Trilium connection has been set up for address: " + settings.triliumAddress + ". " +
                    "You can still change it by tapping the button below.";
        }
    }
}
