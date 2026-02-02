package com.pab.ecommerce_katalog

import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pab.ecommerce_katalog.model.Review
import java.text.NumberFormat
import java.util.Locale

class DetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PRODUCT = "extra_product"
    }

    private var quantity = 1
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var variants: List<ProductVariant>
    private var selectedVariant: ProductVariant? = null

    private lateinit var llSizeContainer: LinearLayout
    private lateinit var llColorContainer: LinearLayout
    private lateinit var tvStockValue: TextView
    private lateinit var tvOutOfStock: TextView
    private lateinit var tvQuantity: TextView
    private lateinit var rvReviews: RecyclerView
    private lateinit var reviewAdapter: ReviewAdapter
    private val reviewList = mutableListOf<Review>()

    private var selectedSizeView: TextView? = null
    private var selectedColorView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        dbHelper = DatabaseHelper(this)

        val ivBack = findViewById<ImageView>(R.id.iv_back)
        val ivProductImage = findViewById<ImageView>(R.id.iv_product_image)
        val tvName = findViewById<TextView>(R.id.tv_name)
        val tvPrice = findViewById<TextView>(R.id.tv_price)
        val ratingBar = findViewById<RatingBar>(R.id.rating_bar)
        val tvRatingText = findViewById<TextView>(R.id.tv_rating_text)
        llSizeContainer = findViewById(R.id.ll_size_container)
        llColorContainer = findViewById(R.id.ll_color_container)
        tvStockValue = findViewById(R.id.tv_stock_value)
        tvOutOfStock = findViewById(R.id.tv_out_of_stock)
        val tvDesc = findViewById<TextView>(R.id.tv_desc)
        val ivMinus = findViewById<ImageView>(R.id.iv_minus)
        val ivPlus = findViewById<ImageView>(R.id.iv_plus)
        tvQuantity = findViewById(R.id.tv_quantity)
        val btnAddToCart = findViewById<Button>(R.id.btn_add_to_cart)
        rvReviews = findViewById(R.id.rv_reviews)

        ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

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
            tvRatingText.text = String.format("%.1f (%d ulasan)", product.rating, product.reviews)
            tvDesc.text = product.description

            Glide.with(this)
                .load(product.imagePath)
                .into(ivProductImage)

            variants = dbHelper.getVariantsForProduct(product.id)
            setupVariantSelectors()
            setupReviewRecyclerView()
            loadReviews(product.id)
        }

        ivMinus.setOnClickListener {
            if (quantity > 1) {
                quantity--
                tvQuantity.text = quantity.toString()
            }
        }

        ivPlus.setOnClickListener {
            quantity++
            tvQuantity.text = quantity.toString()
        }

        btnAddToCart.setOnClickListener {
            selectedVariant?.let {
                val variantId = it.id
                val qty = tvQuantity.text.toString().toInt()
                dbHelper.addToCart(variantId, qty)
                Toast.makeText(this, "Product added to cart", Toast.LENGTH_SHORT).show()
                finish()
            } ?: run {
                Toast.makeText(this, "Please select size and color", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupReviewRecyclerView() {
        reviewAdapter = ReviewAdapter(reviewList)
        rvReviews.layoutManager = LinearLayoutManager(this)
        rvReviews.adapter = reviewAdapter
    }

    private fun loadReviews(productId: Int) {
        val firestore = FirebaseFirestore.getInstance()
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
            .addOnFailureListener {
                // Ignore error for now or show toast
            }
    }

    private fun setupVariantSelectors() {
        val sizes = variants.map { it.size }.distinct()
        llSizeContainer.removeAllViews()
        sizes.forEach { size ->
            val textView = createVariantTextView(size)
            textView.setOnClickListener { onSizeSelected(it as TextView) }
            llSizeContainer.addView(textView)
        }
        (llSizeContainer.getChildAt(0) as? TextView)?.let { onSizeSelected(it) }
    }

    private fun createVariantTextView(text: String): TextView {
        val textView = TextView(this)
        textView.text = text
        textView.setBackgroundResource(R.drawable.variant_background_selector)
        textView.setTextColor(ContextCompat.getColorStateList(this, R.color.variant_text_color))
        textView.gravity = Gravity.CENTER
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 0, 16, 0)
        textView.layoutParams = layoutParams
        textView.setPadding(32, 16, 32, 16)
        return textView
    }

    private fun onSizeSelected(textView: TextView) {
        selectedSizeView?.isSelected = false
        textView.isSelected = true
        selectedSizeView = textView
        updateAvailableColors()
    }

    private fun onColorSelected(textView: TextView) {
        selectedColorView?.isSelected = false
        textView.isSelected = true
        selectedColorView = textView
        updateStock()
    }

    private fun updateAvailableColors() {
        val selectedSize = selectedSizeView?.text?.toString()
        llColorContainer.removeAllViews()
        selectedColorView = null

        if (selectedSize != null) {
            val availableColors = variants.filter { it.size == selectedSize }.map { it.color }.distinct()
            availableColors.forEach { color ->
                val textView = createVariantTextView(color)
                textView.setOnClickListener { onColorSelected(it as TextView) }
                llColorContainer.addView(textView)
            }
            (llColorContainer.getChildAt(0) as? TextView)?.let { onColorSelected(it) }
        } else {
            updateStock()
        }
    }

    private fun updateStock() {
        val selectedSize = selectedSizeView?.text?.toString()
        val selectedColor = selectedColorView?.text?.toString()

        if (selectedSize != null && selectedColor != null) {
            selectedVariant = variants.find { it.size == selectedSize && it.color == selectedColor }

            if (selectedVariant != null) {
                tvStockValue.text = selectedVariant!!.stock.toString()
                if (selectedVariant!!.stock == 0) {
                    tvOutOfStock.text = "Stok Habis"
                    tvOutOfStock.visibility = View.VISIBLE
                    tvStockValue.visibility = View.GONE
                    findViewById<Button>(R.id.btn_add_to_cart).isEnabled = false
                } else {
                    tvOutOfStock.visibility = View.GONE
                    tvStockValue.visibility = View.VISIBLE
                    findViewById<Button>(R.id.btn_add_to_cart).isEnabled = true
                }
            } else {
                tvStockValue.text = ""
                tvOutOfStock.text = "Varian tidak tersedia"
                tvOutOfStock.visibility = View.VISIBLE
                tvStockValue.visibility = View.GONE
                findViewById<Button>(R.id.btn_add_to_cart).isEnabled = false
            }
        } else {
            tvStockValue.text = ""
            tvOutOfStock.text = "Pilih Ukuran & Warna"
            tvOutOfStock.visibility = View.VISIBLE
            tvStockValue.visibility = View.GONE
            findViewById<Button>(R.id.btn_add_to_cart).isEnabled = false
        }
    }
}
