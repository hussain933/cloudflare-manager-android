package com.example.cloudflaremanager

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Skip Welcome if already accepted
        if (PreferencesManager.isTermsAccepted(this)) {
            goToPortSetup()
            return
        }

        setContentView(R.layout.activity_welcome)

        val checkbox = findViewById<CheckBox>(R.id.checkbox_agree)
        val btnContinue = findViewById<Button>(R.id.btn_continue)
        val tvTerms = findViewById<TextView>(R.id.tv_terms)
        val tvPrivacy = findViewById<TextView>(R.id.tv_privacy)

        // Start disabled
        btnContinue.isEnabled = false
        btnContinue.alpha = 0.4f

        checkbox.setOnCheckedChangeListener { _, isChecked ->
            btnContinue.isEnabled = isChecked
            btnContinue.alpha = if (isChecked) 1.0f else 0.4f
        }

        btnContinue.setOnClickListener {
            if (checkbox.isChecked) {
                PreferencesManager.setTermsAccepted(this, true)
                goToPortSetup()
            }
        }

        tvTerms.setOnClickListener {
            startActivity(
                Intent(this, SimpleWebActivity::class.java)
                    .putExtra("url", "file:///android_asset/www/terms.html")
                    .putExtra("title", "Terms of Service")
            )
        }

        tvPrivacy.setOnClickListener {
            startActivity(
                Intent(this, SimpleWebActivity::class.java)
                    .putExtra("url", "file:///android_asset/www/privacy.html")
                    .putExtra("title", "Privacy Policy")
            )
        }
    }

    private fun goToPortSetup() {
        startActivity(Intent(this, PortSetupActivity::class.java))
        finish() // Don't allow back to Welcome
    }
}
