package com.pab.ecommerce_katalog.ui.product

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.pab.ecommerce_katalog.AdminAddEditProductActivity
import com.pab.ecommerce_katalog.AdminProductAdapter
import com.pab.ecommerce_katalog.DatabaseHelper
import com.pab.ecommerce_katalog.Product
import com.pab.ecommerce_katalog.R

class ProductFragment : Fragment() {

    private lateinit var rvProducts: RecyclerView
    private lateinit var fabAddProduct: FloatingActionButton
    private lateinit var productAdapter: AdminProductAdapter
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var searchView: SearchView
    private lateinit var toolbar: Toolbar

    private var allProducts: List<Product> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_product, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())

        toolbar = view.findViewById(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.title = "Manage Products"

        rvProducts = view.findViewById(R.id.rv_products)
        fabAddProduct = view.findViewById(R.id.fab_add_product)
        searchView = view.findViewById(R.id.search_view)

        rvProducts.layoutManager = LinearLayoutManager(context)

        // Menggunakan constructor AdminProductAdapter yang baru
        productAdapter = AdminProductAdapter(
            listOf(),
            requireContext(), // context
            dbHelper,         // dbHelper
            onProductDeleted = {
                loadProducts()
                filterProducts(searchView.query.toString())
            }
        )
        rvProducts.adapter = productAdapter

        fabAddProduct.setOnClickListener {
            startActivity(Intent(context, AdminAddEditProductActivity::class.java))
        }

        setupSearchView()
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