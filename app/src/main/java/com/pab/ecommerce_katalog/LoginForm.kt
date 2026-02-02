package com.pab.ecommerce_katalog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginForm : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_form)

        firebaseAuth = FirebaseAuth.getInstance()
        val dbHelper = DatabaseHelper(this)

        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val txtSignUp = findViewById<TextView>(R.id.txtSignUp)
        val txtForget = findViewById<TextView>(R.id.txtForget)

        txtForget.setOnClickListener {
            startActivity(Intent(this, ForgetPasswordActivity::class.java))
        }

        txtSignUp.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan Password wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Step 1: Sign in with Firebase Authentication (Admin maupun User)
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Jika login sebagai Admin Hardcoded (Pastikan email ini didaftarkan di Firebase Console)
                        if (email == "admin@example.com") {
                            Toast.makeText(this, "Login Admin berhasil", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, AdminDashboard::class.java))
                            finish()
                        } else {
                            // Jika login sebagai User biasa
                            val user = dbHelper.getUserByEmail(email)
                            if (user != null) {
                                val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                with(sharedPref.edit()) {
                                    putString("LOGGED_IN_USERNAME", user.username)
                                    apply()
                                }
                                Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, "Login Firebase berhasil, tapi data lokal tidak ditemukan.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Login Gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
