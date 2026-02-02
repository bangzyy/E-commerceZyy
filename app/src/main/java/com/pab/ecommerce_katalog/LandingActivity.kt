package com.pab.ecommerce_katalog

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LandingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        val btnGetStarted = findViewById<Button>(R.id.btn_get_started)
        btnGetStarted.setOnClickListener {
            startActivity(Intent(this, LoginForm::class.java))
            // Jangan panggil finish() di sini agar user bisa kembali ke landing page saat menekan tombol back
        }
    }
}
