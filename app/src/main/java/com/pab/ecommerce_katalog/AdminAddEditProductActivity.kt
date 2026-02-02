package com.pab.ecommerce_katalog

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText

class AdminAddEditProductActivity : AppCompatActivity() {

    private lateinit var ivProductImage: ImageView
    private lateinit var btnSelectImage: Button
    private lateinit var etProductName: TextInputEditText
    private lateinit var etProductPrice: TextInputEditText
    private lateinit var etProductDescription: TextInputEditText
    private lateinit var actCategory: AutoCompleteTextView
    private lateinit var llVariantsContainer: LinearLayout
    private lateinit var btnAddVariant: Button
    private lateinit var btnSave: Button
    private lateinit var toolbar: Toolbar

    private lateinit var dbHelper: DatabaseHelper
    private var imageUri: Uri? = null
    private var existingProduct: Product? = null

    companion object {
        const val EXTRA_PRODUCT_ID = "extra_product_id"
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
                imageUri = uri
                Glide.with(this).load(imageUri).into(ivProductImage)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add_edit_product)

        dbHelper = DatabaseHelper(this)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ivProductImage = findViewById(R.id.iv_product_image)
        btnSelectImage = findViewById(R.id.btn_select_image)
        etProductName = findViewById(R.id.et_product_name)
        etProductPrice = findViewById(R.id.et_product_price)
        etProductDescription = findViewById(R.id.et_product_description)
        actCategory = findViewById(R.id.act_category)
        llVariantsContainer = findViewById(R.id.ll_variants_container)
        btnAddVariant = findViewById(R.id.btn_add_variant)
        btnSave = findViewById(R.id.btn_save)

        setupCategoryAutoComplete()

        val productId = intent.getIntExtra(EXTRA_PRODUCT_ID, -1)
        Toast.makeText(this, "Received Product ID: $productId", Toast.LENGTH_LONG).show()

        if (productId != -1) {
            supportActionBar?.title = "Edit Product"
            existingProduct = dbHelper.getProductById(productId)
            loadProductData()
        } else {
            supportActionBar?.title = "Add Product"
            addVariantView()
        }

        btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
        }

        btnAddVariant.setOnClickListener { addVariantView() }
        btnSave.setOnClickListener { saveProduct() }
    }

    private fun addVariantView(variant: ProductVariant? = null) {
        val inflater = LayoutInflater.from(this)
        val variantView = inflater.inflate(R.layout.item_variant_input, llVariantsContainer, false)

        val etSize = variantView.findViewById<TextInputEditText>(R.id.et_size)
        val etColor = variantView.findViewById<TextInputEditText>(R.id.et_color)
        val etStock = variantView.findViewById<TextInputEditText>(R.id.et_stock)
        val ivRemoveVariant = variantView.findViewById<ImageView>(R.id.iv_remove_variant)

        variant?.let {
            etSize.setText(it.size)
            etColor.setText(it.color)
            etStock.setText(it.stock.toString())
        }

        ivRemoveVariant.setOnClickListener { llVariantsContainer.removeView(variantView) }

        llVariantsContainer.addView(variantView)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupCategoryAutoComplete() {
        val categories = arrayOf("Man", "Women", "Kids", "New")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        actCategory.setAdapter(adapter)
    }

    private fun loadProductData() {
        existingProduct?.let {
            etProductName.setText(it.name)
            etProductPrice.setText(it.price.toString())
            etProductDescription.setText(it.description)
            actCategory.setText(it.category, false)

            if (it.imagePath.isNotEmpty()) {
                imageUri = Uri.parse(it.imagePath)
                Glide.with(this).load(imageUri).into(ivProductImage)
            }

            val variants = dbHelper.getVariantsForProduct(it.id)
            llVariantsContainer.removeAllViews() // Clear any existing views
            for (variant in variants) {
                addVariantView(variant)
            }
        }
    }

    private fun saveProduct() {
        val name = etProductName.text.toString()
        val price = etProductPrice.text.toString().toDoubleOrNull()
        val description = etProductDescription.text.toString()
        val category = actCategory.text.toString()

        if (name.isBlank() || price == null || description.isBlank() || category.isBlank() || imageUri == null) {
            Toast.makeText(this, "Please fill all product fields and select an image", Toast.LENGTH_SHORT).show()
            return
        }

        val variants = mutableListOf<ProductVariant>()
        for (i in 0 until llVariantsContainer.childCount) {
            val variantView = llVariantsContainer.getChildAt(i)
            val etSize = variantView.findViewById<TextInputEditText>(R.id.et_size)
            val etColor = variantView.findViewById<TextInputEditText>(R.id.et_color)
            val etStock = variantView.findViewById<TextInputEditText>(R.id.et_stock)

            val size = etSize.text.toString()
            val color = etColor.text.toString()
            val stock = etStock.text.toString().toIntOrNull()

            if (size.isBlank() || color.isBlank() || stock == null) {
                Toast.makeText(this, "Please fill all variant fields", Toast.LENGTH_SHORT).show()
                return
            }
            variants.add(ProductVariant(id = 0, productId = existingProduct?.id ?: 0, size = size, color = color, stock = stock))
        }

        if (variants.isEmpty()) {
            Toast.makeText(this, "Please add at least one product variant", Toast.LENGTH_SHORT).show()
            return
        }

        val product = Product(
            id = existingProduct?.id ?: 0,
            name = name,
            price = price,
            description = description,
            category = category,
            imagePath = imageUri.toString(),
            rating = existingProduct?.rating ?: 0.0f, // Preserve original rating and reviews
            reviews = existingProduct?.reviews ?: 0
        )

        if (existingProduct == null) {
            // Add new product
            dbHelper.addProductWithVariants(product, variants)
            Toast.makeText(this, "Product added successfully", Toast.LENGTH_SHORT).show()
        } else {
            // Update existing product
            dbHelper.updateProductWithVariants(product, variants)
            Toast.makeText(this, "Product updated successfully", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}