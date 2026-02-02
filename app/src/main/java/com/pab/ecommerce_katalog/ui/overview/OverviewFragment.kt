package com.pab.ecommerce_katalog.ui.overview

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pab.ecommerce_katalog.DatabaseHelper
import com.pab.ecommerce_katalog.LoginForm
import com.pab.ecommerce_katalog.R
import java.text.NumberFormat
import java.util.Locale

class OverviewFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val tvTotalUsers = view.findViewById<TextView>(R.id.tv_total_users)
        val tvTotalProducts = view.findViewById<TextView>(R.id.tv_total_products)
        val tvTotalSales = view.findViewById<TextView>(R.id.tv_total_sales)
        
        val cardSales = view.findViewById<MaterialCardView>(R.id.card_sales)
        val cardProducts = view.findViewById<MaterialCardView>(R.id.card_products)
        val cardUsers = view.findViewById<MaterialCardView>(R.id.card_users)
        val btnManageOrders = view.findViewById<MaterialCardView>(R.id.btn_manage_orders)
        val btnLogout = view.findViewById<MaterialButton>(R.id.btn_logout_admin)

        // Load Products from Local DB
        tvTotalProducts.text = dbHelper.getTotalProducts().toString()

        // Fetch Total Users from Firestore (Exclude Admin)
        fetchTotalUsers(tvTotalUsers)

        // Fetch Firestore Sales Data
        fetchTotalSales(tvTotalSales)

        // Navigation Listeners
        cardSales.setOnClickListener { findNavController().navigate(R.id.acceptedProductsFragment) }
        btnManageOrders.setOnClickListener { findNavController().navigate(R.id.adminOrderFragment) }
        cardProducts.setOnClickListener { findNavController().navigate(R.id.productFragment) }
        cardUsers.setOnClickListener { findNavController().navigate(R.id.usersFragment) }
        
        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireActivity(), LoginForm::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun fetchTotalUsers(tvUsers: TextView) {
        firestore.collection("users")
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    val userCount = snapshots.documents.count { doc ->
                        val email = doc.getString("email")
                        email != "admin@example.com"
                    }
                    tvUsers.text = userCount.toString()
                }
            }
    }

    private fun fetchTotalSales(tvSales: TextView) {
        firestore.collection("orders")
            .whereEqualTo("status", "Accepted")
            .addSnapshotListener { snapshots, _ ->
                var total: Long = 0
                if (snapshots != null) {
                    for (doc in snapshots.documents) {
                        total += doc.getLong("totalHarga") ?: 0L
                    }
                }
                val localeID = Locale.forLanguageTag("in-ID")
                val numberFormat = NumberFormat.getCurrencyInstance(localeID)
                numberFormat.maximumFractionDigits = 0
                tvSales.text = numberFormat.format(total)
            }
    }
}
