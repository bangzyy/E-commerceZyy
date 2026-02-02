package com.pab.ecommerce_katalog

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AdminProductActivity : AppCompatActivity() {

    private lateinit var rvProducts: RecyclerView
    private lateinit var fabAddProduct: FloatingActionButton
    private lateinit var productAdapter: AdminProductAdapter
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var searchView: SearchView
    private lateinit var toolbar: Toolbar

    private var allProducts: List<Product> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_product)

        dbHelper = DatabaseHelper(this)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Products"

        rvProducts = findViewById(R.id.rv_products)
        fabAddProduct = findViewById(R.id.fab_add_product)
        searchView = findViewById(R.id.search_view)

        rvProducts.layoutManager = LinearLayoutManager(this)

        // Membuat adapter dengan cara baru yang lebih sederhana
        productAdapter = AdminProductAdapter(
            listOf(),
            this, // Memberikan context ke adapter
            dbHelper, // Memberikan dbHelper ke adapter
            onProductDeleted = {
                // Adapter akan memanggil ini setelah produk dihapus
                loadProducts()
                filterProducts(searchView.query.toString())
            }
        )
        rvProducts.adapter = productAdapter

        fabAddProduct.setOnClickListener {
            startActivity(Intent(this, AdminAddEditProductActivity::class.java))
        }

        setupSearchView()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterProducts(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterProducts(newText)
                return true
            }
        })
    }

    private fun filterProducts(query: String?) {
        val filteredList = if (query.isNullOrEmpty()) {
            allProducts
        } else {
            allProducts.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
        productAdapter.updateProducts(filteredList)
    }

    private fun loadProducts() {
        allProducts = dbHelper.getAllProducts()
        productAdapter.updateProducts(allProducts)
    }

    override fun onResume() {
        super.onResume()
        loadProducts()
        filterProducts(searchView.query.toString())
    }
}