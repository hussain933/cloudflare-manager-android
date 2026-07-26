package com.example.cloudflaremanager

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PortSetupActivity : AppCompatActivity() {

    private var fromSettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_port_setup)

        fromSettings = intent.getBooleanExtra("fromSettings", false)

        val etPort = findViewById<EditText>(R.id.et_port)
        val btnStart = findViewById<Button>(R.id.btn_start)
        val tvError = findViewById<TextView>(R.id.tv_error)

        // Pre-fill saved port
        etPort.setText(PreferencesManager.getLocalServerPort(this).toString())

        etPort.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val port = s.toString().toIntOrNull()
                val valid = port != null && port in 1024..65535
                tvError.text = if (!valid && s.toString().isNotEmpty())
                    "Port must be between 1024 and 65535" else ""
                btnStart.isEnabled = valid
                btnStart.alpha = if (valid) 1.0f else 0.4f
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnStart.setOnClickListener {
            val port = etPort.text.toString().toIntOrNull()
            if (port == null || port !in 1024..65535) {
                tvError.text = "Port must be between 1024 and 65535"
                return@setOnClickListener
            }
            PreferencesManager.setLocalServerPort(this, port)

            if (fromSettings) {
                // Just go back; loader will restart server on next launch
                finish()
            } else {
                startActivity(Intent(this, LoaderActivity::class.java))
                finish()
            }
        }
    }
}
