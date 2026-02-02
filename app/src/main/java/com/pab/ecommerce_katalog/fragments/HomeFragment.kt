package com.pab.ecommerce_katalog.fragments

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pab.ecommerce_katalog.DatabaseHelper
import com.pab.ecommerce_katalog.DetailActivity
import com.pab.ecommerce_katalog.Product
import com.pab.ecommerce_katalog.ProductAdapter
import com.pab.ecommerce_katalog.R

class HomeFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var productAdapter: ProductAdapter
    private lateinit var recyclerView: RecyclerView

    private lateinit var btnMan: Button
    private lateinit var btnWomen: Button
    private lateinit var btnKids: Button
    private lateinit var btnNew: Button
    private lateinit var buttons: List<Button>

    private var currentCategory: String = "Man"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        dbHelper = DatabaseHelper(requireContext())

        recyclerView = view.findViewById(R.id.rvProducts)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        productAdapter = ProductAdapter(emptyList()) { product ->
            val intent = Intent(requireContext(), DetailActivity::class.java).apply {
                putExtra(DetailActivity.EXTRA_PRODUCT, product)
            }
            startActivity(intent)
        }
        recyclerView.adapter = productAdapter

        // Initialize buttons
        btnMan = view.findViewById(R.id.btnMan)
        btnWomen = view.findViewById(R.id.btnWomen)
        btnKids = view.findViewById(R.id.btnKids)
        btnNew = view.findViewById(R.id.btnNew)
        buttons = listOf(btnMan, btnWomen, btnKids, btnNew)

        // Set up category buttons
        btnMan.setOnClickListener { onCategoryButtonClick(btnMan, "Man") }
        btnWomen.setOnClickListener { onCategoryButtonClick(btnWomen, "Women") }
        btnKids.setOnClickListener { onCategoryButtonClick(btnKids, "Kids") }
        btnNew.setOnClickListener { onCategoryButtonClick(btnNew, "New") }

        // Load "Man" products initially and set the initial active button
        onCategoryButtonClick(btnMan, "Man")

        return view
    }

    private fun onCategoryButtonClick(clickedButton: Button, category: String) {
        currentCategory = category
        updateButtonStyles(clickedButton)
        filterProductsByCategory(category)
    }

    private fun updateButtonStyles(activeButton: Button?) {
        val activeTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_active))
        val inactiveTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.color_primary))

        for (button in buttons) {
            if (button == activeButton) {
                button.backgroundTintList = activeTint
                button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            } else {
                button.backgroundTintList = inactiveTint
                button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            }
        }
    }

    private fun filterProductsByCategory(category: String) {
        val productList = if (category == "All") {
            dbHelper.getAllProducts()
        } else {
            dbHelper.getProductsByCategory(category)
        }
        productAdapter.updateProducts(productList)
    }

    fun filterProductsByName(query: String?) {
        val allProducts = dbHelper.getAllProducts()
        val filteredList = if (query.isNullOrBlank()) {
            dbHelper.getProductsByCategory(currentCategory)
        } else {
            allProducts.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
        productAdapter.updateProducts(filteredList)

        if (!query.isNullOrBlank()) {
            updateButtonStyles(null)
        } else {
            val activeButton = when (currentCategory) {
                "Man" -> btnMan
                "Women" -> btnWomen
                "Kids" -> btnKids
                "New" -> btnNew
                else -> null
            }
            updateButtonStyles(activeButton)
        }
    }
}