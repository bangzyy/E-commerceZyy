package com.pab.ecommerce_katalog

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pab.ecommerce_katalog.model.Review
import java.text.NumberFormat
import java.util.Locale

class AdminProductDetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PRODUCT = "extra_product"
    }

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var firestore: FirebaseFirestore
    private lateinit var rvVariants: RecyclerView
    private lateinit var rvReviews: RecyclerView
    private lateinit var reviewAdapter: ReviewAdapter
    private val reviewList = mutableListOf<Review>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_product_detail)

        dbHelper = DatabaseHelper(this)
        firestore = FirebaseFirestore.getInstance()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        val ivProductImage = findViewById<ImageView>(R.id.iv_product_image)
        val tvName = findViewById<TextView>(R.id.tv_name)
        val tvPrice = findViewById<TextView>(R.id.tv_price)
        val ratingBar = findViewById<RatingBar>(R.id.rating_bar)
        val tvRatingCount = findViewById<TextView>(R.id.tv_rating_count)
        val tvDesc = findViewById<TextView>(R.id.tv_desc)
        rvVariants = findViewById(R.id.rv_variants)
        rvReviews = findViewById(R.id.rv_reviews)

        val product = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_PRODUCT, Product::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Product>(EXTRA_PRODUCT)
        }

        if (product != null) {
            tvName.text = product.name
            
            val localeID = Locale.forLanguageTag("in-ID")
            val numberFormat = NumberFormat.getCurrencyInstance(localeID)
            numberFormat.maximumFractionDigits = 0
            tvPrice.text = numberFormat.format(product.price)

            ratingBar.rating = product.rating
            tvRatingCount.text = String.format("%.1f (%d ulasan)", product.rating, product.reviews)
            tvDesc.text = product.description

            Glide.with(this)
                .load(product.imagePath)
                .placeholder(R.drawable.ic_product)
                .into(ivProductImage)

            setupVariantsList(product.id)
            setupReviewRecyclerView()
            loadReviews(product.id)
        }

        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupVariantsList(productId: Int) {
        val variants = dbHelper.getVariantsForProduct(productId)
        rvVariants.layoutManager = LinearLayoutManager(this)
        rvVariants.adapter = object : RecyclerView.Adapter<VariantViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VariantViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_variant_display, parent, false)
                return VariantViewHolder(view)
            }
            override fun onBindViewHolder(holder: VariantViewHolder, position: Int) {
                val variant = variants[position]
                holder.tvInfo.text = "Size: ${variant.size} | Color: ${variant.color}"
                holder.tvStock.text = "Stok: ${variant.stock}"
            }
            override fun getItemCount() = variants.size
        }
    }

    private fun setupReviewRecyclerView() {
        reviewAdapter = ReviewAdapter(reviewList)
        rvReviews.layoutManager = LinearLayoutManager(this)
        rvReviews.adapter = reviewAdapter
    }

    private fun loadReviews(productId: Int) {
        firestore.collection("products")
            .document(productId.toString())
            .collection("reviews")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                reviewList.clear()
                for (document in documents) {
                    val review = document.toObject(Review::class.java)
                    reviewList.add(review)
                }
                reviewAdapter.notifyDataSetChanged()
            }
    }

    class VariantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInfo: TextView = view.findViewById(R.id.tv_variant_info)
        val tvStock: TextView = view.findViewById(R.id.tv_stock)
    }
}
