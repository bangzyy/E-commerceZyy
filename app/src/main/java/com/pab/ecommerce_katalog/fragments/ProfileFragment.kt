package com.pab.ecommerce_katalog.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pab.ecommerce_katalog.CompleteProfileActivity
import com.pab.ecommerce_katalog.HistoryAdapter
import com.pab.ecommerce_katalog.LandingActivity
import com.pab.ecommerce_katalog.model.Order
import com.pab.ecommerce_katalog.R
import de.hdodenhof.circleimageview.CircleImageView
import com.bumptech.glide.signature.ObjectKey

import java.io.File

class ProfileFragment : Fragment() {

    private lateinit var ivProfileImage: CircleImageView
    private lateinit var tvEmail: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvPhoneNumber: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var btnLogout: Button
    private lateinit var rvHistory: RecyclerView

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var historyAdapter: HistoryAdapter
    private val historyList = mutableListOf<Order>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        ivProfileImage = view.findViewById(R.id.ivProfileImage)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvUsername = view.findViewById(R.id.tvUsername)
        tvAddress = view.findViewById(R.id.tvAddress)
        tvPhoneNumber = view.findViewById(R.id.tvPhoneNumber)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnLogout = view.findViewById(R.id.btnLogout)
        rvHistory = view.findViewById(R.id.rvHistory)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupRecyclerView()

        btnEditProfile.setOnClickListener {
            val intent = Intent(activity, CompleteProfileActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(activity, LandingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
        loadOrderHistory()
    }

    private fun setupRecyclerView() {
        // historyList di sini sekarang resmi menggunakan com.pab.ecommerce_katalog.model.Order
        historyAdapter = HistoryAdapter(historyList)
        rvHistory.layoutManager = LinearLayoutManager(context)
        rvHistory.adapter = historyAdapter
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            tvEmail.text = user.email
            val userDocRef = firestore.collection("users").document(user.uid)
            userDocRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val username = document.getString("username")
                        val address = document.getString("address")
                        val phoneNumber = document.getString("phone")
                        val profileImagePath = document.getString("profileImageUrl")

                        tvUsername.text = username ?: "User"
                        tvAddress.text = "Address: ${address ?: "Belum diisi"}"
                        tvPhoneNumber.text = "No.HP: ${phoneNumber ?: "Belum diisi"}"

                        if (!profileImagePath.isNullOrEmpty()) {
                            val imageFile = File(profileImagePath)
                            if (imageFile.exists()) {
                                Glide.with(this@ProfileFragment)
                                    .load(imageFile)
                                    .signature(ObjectKey(imageFile.lastModified()))
                                    .into(ivProfileImage)
                            } else {
                                ivProfileImage.setImageResource(R.drawable.ic_launcher_background)
                            }
                        } else {
                            ivProfileImage.setImageResource(R.drawable.ic_launcher_background)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    if (isAdded) {
                        Toast.makeText(activity, "Gagal memuat data profil: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun loadOrderHistory() {
        val user = auth.currentUser
        if (user != null) {
            firestore.collection("orders")
                .whereEqualTo("userId", user.uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    if (!isAdded) return@addOnSuccessListener
                    
                    historyList.clear()
                    for (document in documents) {
                        try {
                            val order = document.toObject(Order::class.java)
                            if (order != null) {
                                order.id = document.id
                                historyList.add(order)
                            }
                        } catch (e: Exception) {
                            // Skip broken data
                        }
                    }
                    historyAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    if (isAdded) {
                        Toast.makeText(activity, "Gagal memuat riwayat pesanan: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
