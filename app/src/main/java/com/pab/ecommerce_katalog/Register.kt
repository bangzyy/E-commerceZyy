package com.pab.ecommerce_katalog

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class Register : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var dbHelper: DatabaseHelper

    private val TAG = "GoogleSignIn"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        firebaseAuth = FirebaseAuth.getInstance()
        dbHelper = DatabaseHelper(this)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Make sure this string resource exists
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // --- Event Listeners ---
        findViewById<SignInButton>(R.id.btnGoogleSignIn).setOnClickListener {
            signInWithGoogle()
        }

        findViewById<Button>(R.id.btnSignUp).setOnClickListener {
            registerWithEmailPassword()
        }
    }

    private fun registerWithEmailPassword() {
        val edtUsername = findViewById<EditText>(R.id.edtUsername)
        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val edtAddress = findViewById<EditText>(R.id.edtAddress)
        val edtTel = findViewById<EditText>(R.id.edtTel)

        val username = edtUsername.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()
        val address = edtAddress.text.toString().trim()
        val phone = edtTel.text.toString().trim()

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || address.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Semua data wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    firebaseUser?.sendEmailVerification()
                        ?.addOnCompleteListener { verificationTask ->
                            val toastMessage = if (verificationTask.isSuccessful) {
                                dbHelper.insertUser(username, email, password, address, phone)
                                "Registrasi berhasil! Silakan cek email Anda untuk verifikasi."
                            } else {
                                "Registrasi berhasil, namun gagal mengirim email verifikasi."
                            }
                            Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
                            finish()
                        }
                } else {
                    Toast.makeText(this, "Registrasi Gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Google Sign-in gagal: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        val email = user.email!!
                        val username = user.displayName ?: ""

                        val intent: Intent
                        if (!dbHelper.checkEmail(email)) {
                            // This is a new user
                            dbHelper.insertUser(username, email, "", "", "")
                            intent = Intent(this, CompleteProfileActivity::class.java).apply {
                                putExtra("USER_NAME", username)
                                putExtra("USER_EMAIL", email)
                            }
                        } else {
                            // This is a returning user
                            intent = Intent(this, MainActivity::class.java)
                        }
                        startActivity(intent)
                        finishAffinity()
                    }
                } else {
                    Toast.makeText(this, "Otentikasi Firebase Gagal.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
