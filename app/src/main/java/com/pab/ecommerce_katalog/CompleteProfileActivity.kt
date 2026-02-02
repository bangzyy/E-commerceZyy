package com.pab.ecommerce_katalog

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import java.io.FileOutputStream
import com.bumptech.glide.signature.ObjectKey

class CompleteProfileActivity : AppCompatActivity() {

    private lateinit var ivProfileImage: CircleImageView
    private lateinit var btnChangeImage: ImageButton
    private lateinit var edtUsername: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtAddress: EditText
    private lateinit var edtTel: EditText
    private lateinit var btnSaveProfile: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var imageUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageUri = result.data?.data
            ivProfileImage.setImageURI(imageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complete_profile)

        ivProfileImage = findViewById(R.id.ivProfileImage)
        btnChangeImage = findViewById(R.id.btnChangeImage)
        edtUsername = findViewById(R.id.edtUsername)
        edtEmail = findViewById(R.id.edtEmail)
        edtAddress = findViewById(R.id.edtAddress)
        edtTel = findViewById(R.id.edtTel)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        loadUserProfile()

        btnChangeImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            galleryLauncher.launch(intent)
        }

        btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            edtEmail.setText(user.email)
            val userDocRef = firestore.collection("users").document(user.uid)
            userDocRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    edtUsername.setText(document.getString("username"))
                    edtAddress.setText(document.getString("address"))
                    edtTel.setText(document.getString("phone"))
                    val profileImageUrl = document.getString("profileImageUrl")
                    if (!profileImageUrl.isNullOrEmpty()) {
                        val imageFile = File(profileImageUrl)
                        Glide.with(this)
                            .load(imageFile)
                            .signature(ObjectKey(imageFile.lastModified()))
                            .into(ivProfileImage)
                    }
                }
            }
        }
    }

    private fun saveProfile() {
        val username = edtUsername.text.toString().trim()
        val address = edtAddress.text.toString().trim()
        val phone = edtTel.text.toString().trim()

        if (username.isEmpty() || address.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Semua data wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser
        if (user != null) {
            if (imageUri != null) {
                // Simpan gambar ke penyimpanan internal dan dapatkan path lokalnya
                val localImagePath = saveImageToInternalStorage(imageUri!!, user.uid)
                if (localImagePath != null) {
                    saveProfileData(user.uid, username, address, phone, localImagePath)
                } else {
                    Toast.makeText(this, "Gagal menyimpan gambar.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Simpan data profil tanpa mengubah gambar
                saveProfileData(user.uid, username, address, phone, null)
            }
        }
    }

    private fun saveImageToInternalStorage(uri: Uri, userId: String): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(filesDir, "${userId}_profile.jpg") // Simpan dengan nama unik
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file.absolutePath // Kembalikan path absolut dari file yang disimpan
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveProfileData(userId: String, username: String, address: String, phone: String, imagePath: String?) {
        val userDocRef = firestore.collection("users").document(userId)
        val userData = mutableMapOf<String, Any>(
            "username" to username,
            "address" to address,
            "phone" to phone
        )
        // Hanya perbarui imagePath jika ada path baru yang disediakan
        if (imagePath != null) {
            userData["profileImageUrl"] = imagePath
        }

        // Gunakan SetOptions.merge() untuk membuat dokumen jika belum ada, atau memperbarui jika sudah ada
        userDocRef.set(userData, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memperbarui profil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}