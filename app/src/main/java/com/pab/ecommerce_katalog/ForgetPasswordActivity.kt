package com.pab.ecommerce_katalog

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ForgetPasswordActivity : AppCompatActivity() {

    private lateinit var edtEmail: EditText
    private lateinit var btnReset: Button
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget_password)

        edtEmail = findViewById(R.id.edtEmail)
        btnReset = findViewById(R.id.btnReset)
        firebaseAuth = FirebaseAuth.getInstance()

        btnReset.setOnClickListener {
            val email = edtEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Silakan masukkan alamat email Anda", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendPasswordResetEmail(email)
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Email pengaturan ulang kata sandi telah dikirim. Silakan periksa kotak masuk Anda.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish() // Close the activity after sending the email
                } else {
                    val errorMessage = task.exception?.message ?: "Gagal mengirim email."
                    Toast.makeText(
                        this,
                        "Error: $errorMessage",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}
