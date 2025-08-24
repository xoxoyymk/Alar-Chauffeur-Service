package com.alar.chauffeurservice.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deepUri: Uri? = intent?.data
        val main = Intent(this, MainActivity::class.java).apply {
            if (deepUri != null) data = deepUri
        }
        startActivity(main)
        finish()
    }
}