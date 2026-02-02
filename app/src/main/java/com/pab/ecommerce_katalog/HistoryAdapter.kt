package com.pab.ecommerce_katalog

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pab.ecommerce_katalog.model.Order
import com.pab.ecommerce_katalog.model.Review
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(private val historyList: List<Order>) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val order = historyList[position]
        holder.bind(order)
    }

    override fun getItemCount(): Int {
        return historyList.size
    }

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvOrderStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
        private val tvOrderDate: TextView = itemView.findViewById(R.id.tvOrderDate)
        private val tvOrderTotal: TextView = itemView.findViewById(R.id.tvOrderTotal)
        private val tvOrderItems: TextView = itemView.findViewById(R.id.tvOrderItems)
        private val ivProductFirst: ImageView = itemView.findViewById(R.id.ivProductFirst)
        private val tvProductDetails: TextView = itemView.findViewById(R.id.tvProductDetails)
        private val tvProductQuantity: TextView = itemView.findViewById(R.id.tvProductQuantity)
        private val tvMoreItems: TextView = itemView.findViewById(R.id.tvMoreItems)
        private val btnAction: MaterialButton = itemView.findViewById(R.id.btnReorder)

        fun bind(order: Order) {
            val status = order.status.lowercase()
            tvOrderStatus.text = order.status.uppercase()
            
            when (status) {
                "accepted" -> {
                    tvOrderStatus.background.setTint(itemView.context.getColor(android.R.color.holo_green_light))
                    tvOrderStatus.setTextColor(itemView.context.getColor(android.R.color.white))
                    
                    if (order.rated) {
                        btnAction.text = "Sudah Dinilai"
                        btnAction.isEnabled = false
                        btnAction.alpha = 0.6f
                        btnAction.visibility = View.VISIBLE
                    } else {
                        btnAction.text = "Beri Rating"
                        btnAction.isEnabled = true
                        btnAction.alpha = 1.0f
                        btnAction.visibility = View.VISIBLE
                    }
                }
                "pending" -> {
                    tvOrderStatus.background.setTint(itemView.context.getColor(android.R.color.holo_orange_light))
                    tvOrderStatus.setTextColor(itemView.context.getColor(android.R.color.white))
                    btnAction.visibility = View.GONE
                }
                else -> {
                    btnAction.text = "Beli Lagi"
                    btnAction.visibility = View.VISIBLE
                }
            }
            
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            tvOrderDate.text = order.timestamp?.let { sdf.format(it) } ?: "-"
            
            tvOrderTotal.text = "Rp ${String.format("%,.0f", order.totalHarga.toDouble())}"

            if (order.products.isNotEmpty()) {
                val firstProduct = order.products[0]
                tvOrderItems.text = firstProduct.productName
                tvProductQuantity.text = "${firstProduct.quantity} barang"
                
                val detailText = StringBuilder()
                if (firstProduct.size.isNotEmpty()) detailText.append("Size: ${firstProduct.size}")
                if (firstProduct.color.isNotEmpty()) {
                    if (detailText.isNotEmpty()) detailText.append(" | ")
                    detailText.append("Color: ${firstProduct.color}")
                }
                
                tvProductDetails.text = detailText.toString()
                tvProductDetails.visibility = if (detailText.isNotEmpty()) View.VISIBLE else View.GONE

                Glide.with(itemView.context)
                    .load(firstProduct.imagePath)
                    .placeholder(R.drawable.ic_product)
                    .into(ivProductFirst)

                if (order.products.size > 1) {
                    tvMoreItems.visibility = View.VISIBLE
                    tvMoreItems.text = "+ ${order.products.size - 1} produk lainnya"
                } else {
                    tvMoreItems.visibility = View.GONE
                }

                btnAction.setOnClickListener {
                    if (status == "accepted" && !order.rated) {
                        showRatingDialog(order)
                    } else if (status != "accepted" && status != "pending") {
                        Toast.makeText(itemView.context, "Fitur Beli Lagi segera hadir", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        private fun showRatingDialog(order: Order) {
            val context = itemView.context
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_review, null)
            val tvProductName = dialogView.findViewById<TextView>(R.id.tvProductName)
            val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
            val etReview = dialogView.findViewById<EditText>(R.id.etReview)

            val firstProduct = order.products[0]
            tvProductName.text = firstProduct.productName

            AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton("Kirim") { dialog, _ ->
                    val rating = ratingBar.rating
                    val reviewText = etReview.text.toString()
                    
                    if (rating > 0) {
                        saveRatingToDatabase(order, firstProduct.productId, rating, reviewText)
                        dialog.dismiss()
                    } else {
                        Toast.makeText(context, "Berikan rating bintang terlebih dahulu", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        private fun saveRatingToDatabase(order: Order, productId: Int, rating: Float, comment: String) {
            val dbHelper = DatabaseHelper(itemView.context)
            val success = dbHelper.updateProductRating(productId, rating)
            
            if (success) {
                val auth = FirebaseAuth.getInstance()
                val firestore = FirebaseFirestore.getInstance()
                val currentUser = auth.currentUser
                
                if (currentUser != null && productId != -1) {
                    val sharedPref = itemView.context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    val userName = sharedPref.getString("LOGGED_IN_USERNAME", "User") ?: "User"
                    
                    val review = Review(
                        userId = currentUser.uid,
                        userName = userName,
                        rating = rating,
                        comment = comment
                    )
                    
                    // Simpan Review ke Produk
                    firestore.collection("products")
                        .document(productId.toString())
                        .collection("reviews")
                        .add(review)

                    // Update Status Pesanan Menjadi SUDAH DIRATING agar tombol hilang
                    order.id?.let { orderId ->
                        firestore.collection("orders").document(orderId)
                            .update("rated", true)
                            .addOnSuccessListener {
                                Toast.makeText(itemView.context, "Terima kasih atas ulasan Anda!", Toast.LENGTH_SHORT).show()
                                btnAction.text = "Sudah Dinilai"
                                btnAction.isEnabled = false
                                btnAction.alpha = 0.6f
                            }
                    }
                }
            } else {
                Toast.makeText(itemView.context, "Gagal menyimpan rating.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
